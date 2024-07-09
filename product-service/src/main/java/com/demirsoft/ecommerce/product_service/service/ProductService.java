package com.demirsoft.ecommerce.product_service.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demirsoft.ecommerce.product_service.config.KafkaConsumerConfig;
import com.demirsoft.ecommerce.product_service.entity.InventoryRequestStatus;
import com.demirsoft.ecommerce.product_service.entity.InventoryUpdateLog;
import com.demirsoft.ecommerce.product_service.entity.Product;
import com.demirsoft.ecommerce.product_service.event.InventoryAllocated;
import com.demirsoft.ecommerce.product_service.event.OrderCreated;
import com.demirsoft.ecommerce.product_service.event.OrderFailed;
import com.demirsoft.ecommerce.product_service.event.OrderFailed.ServiceType;
import com.demirsoft.ecommerce.product_service.exception.OrderAlreadyProcessedException;
import com.demirsoft.ecommerce.product_service.exception.ProductNotFoundException;
import com.demirsoft.ecommerce.product_service.repository.InventoryRequestRepository;
import com.demirsoft.ecommerce.product_service.repository.ProductRepository;
import com.demirsoft.ecommerce.product_service.service.helpers.ProductQuantityProcessor;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Log4j2
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // requieres mongo replicaset, so disabled temporarily
    // @Autowired
    // private TransactionalOperator transactionalOperator;

    @Autowired
    private InventoryRequestRepository inventoryRequestRepository;

    @Autowired
    @Qualifier("OverwriteProductWithoutId")
    public ModelMapper modelMapperForOverwriteProductWithoutId;

    @Autowired
    private ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;
    // private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String ORDER_CREATED = "order_created";
    private static final String ORDER_FAILED = "order_failed";
    private static final String INVENTORY_ALLOCATED = "inventory_allocated";

    public Mono<Product> findProductById(String id) {
        return productRepository.findById(id);
    }

    public Flux<Product> findAllProducts() {
        return productRepository.findAll();
    }

    public Flux<Product> findProductsByName(String name) {
        return productRepository.findByName(name);
    }

    public Mono<Void> deleteProductById(String id) {
        return productRepository.deleteById(id);
    }

    public Mono<Product> createProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public Mono<Product> updateProduct(Product newProduct) {

        return productRepository.findById(newProduct.getId())
                .switchIfEmpty(
                        Mono.error(new ProductNotFoundException(String.format("product id: %s", newProduct.getId()))))
                .flatMap(dbProduct -> {
                    modelMapperForOverwriteProductWithoutId.map(newProduct, dbProduct);
                    return productRepository.save(dbProduct);
                });
    }

    @KafkaListener(topics = ORDER_CREATED, groupId = KafkaConsumerConfig.CONSUMER_GROUP, autoStartup = "true", containerFactory = "kafkaListenerContainerFactory")
    public void consume(@Payload OrderCreated order, Acknowledgment ack) {

        log.info("order topic received: " + order.toString());

        checkForDuplicateOrderRequest(order)
                .then(processRequest(order))
                .subscribe(
                        result -> log.debug(String.format("order topic processing successful: %s", order.toString())),
                        error -> log.error(
                                String.format("order topic processing failed %s: reason: %s", order.toString(), error)),
                        () -> ack.acknowledge());
    }

    private Mono<Boolean> checkForDuplicateOrderRequest(OrderCreated order) {

        return getInventoryUpdateLogsForThisOrder(order)
                .count()
                .flatMap((count) -> {
                    if (count > 0) {
                        return Mono.<Boolean>error(
                                new OrderAlreadyProcessedException(String.format("order id: %s", order.getId())));

                    }

                    return Mono.just(false);
                });

    }

    private Mono<Boolean> processRequest(OrderCreated order) {

        var quantityProcessor = new ProductQuantityProcessor(order);

        return productRepository.findAllById(quantityProcessor.getProductIds()).collectList()
                .flatMapMany(products -> {

                    quantityProcessor.checkAndDecrementProductQuantities(products);

                    if (quantityProcessor.areThereMissingItems()) {
                        rejectOrder(order, quantityProcessor.getMissingItems());
                        return Mono.empty();
                    }

                    return acceptOrder(order, products);
                })
                .next();

    }

    @Transactional
    private void rejectOrder(OrderCreated order, List<String> reason) {

        var inventoryUpdateLog = buildInventoryUnavailableLog(order);
        inventoryRequestRepository.save(inventoryUpdateLog);

        var orderFailed = buildOrderFailed(order, reason);
        kafkaTemplate.send(ORDER_FAILED, orderFailed.getId(), orderFailed);

    }

    private OrderFailed buildOrderFailed(OrderCreated order, List<String> reason) {
        // setting null id creates a new log for this order id
        return new OrderFailed(
                order.getId(),
                order.getCustomerId(),
                ServiceType.INVENTORY_SERVICE.name(),
                reason);
    }

    @Transactional
    private Mono<Boolean> acceptOrder(OrderCreated order, List<Product> products) {
        // update product repo with new quantities
        AtomicBoolean dbOperationsSaved = new AtomicBoolean(false);

        return updateProductRepoAndInventoryUpdateLogAtomically(products, order)
                // requieres mongo replicaset, so disabled temporarily
                // .as(transactionalOperator::transactional)
                .flatMap(prev_step_ok -> {
                    dbOperationsSaved.set(true);

                    return sendInventoryAllocatedEvent(order);
                })
                .then(Mono.just(true))
                .doOnError(one_of_steps_failed -> {
                    // rollback successful steps,
                    if (dbOperationsSaved.get())
                        rollbackProductRepoAndInventoryUpdateLogAtomically(products, order);

                    // kafka step is already not processed or failed
                })
                .then(Mono.just(false));
    }

    private Flux<InventoryUpdateLog> getInventoryUpdateLogsForThisOrder(OrderCreated order) {
        return inventoryRequestRepository.findByOrderId(order.getId());
    }

    private Mono<Boolean> updateProductRepoAndInventoryUpdateLogAtomically(
            List<Product> products,
            OrderCreated order) {
        var productsSaveRequest = productRepository.saveAll(products);
        var saveInventoryUpdateLogRequest = inventoryRequestRepository.save(buildInventoryAllocatedLog(order));

        return productsSaveRequest
                .then(saveInventoryUpdateLogRequest)
                // requieres mongo replicaset, so disabled temporarily
                // .as(transactionalOperator::transactional)
                .then(Mono.just(true));
    }

    private void rollbackProductRepoAndInventoryUpdateLogAtomically(List<Product> products, OrderCreated order) {
        throw new UnsupportedOperationException("Unimplemented method 'rollbackInventoryRepo'");
    }

    private InventoryUpdateLog buildInventoryUnavailableLog(OrderCreated order) {
        // setting null id creates a new log for each request
        return new InventoryUpdateLog(null, order.getId(), InventoryRequestStatus.ITEMS_UNAVAILABLE);
    }

    private InventoryUpdateLog buildInventoryAllocatedLog(OrderCreated order) {
        // setting null id creates a new log for each request
        return new InventoryUpdateLog(null, order.getId(), InventoryRequestStatus.ITEMS_ALLOCATED);
    }

    private Mono<Boolean> sendInventoryAllocatedEvent(OrderCreated order) {

        var inventoryAllocated = new InventoryAllocated(
                order.getId(), order.getCustomerId(), order.getCreditCardInfo(),
                order.getPrice(), order.getShippingAddress());

        var kafkaRequest = kafkaTemplate.send(INVENTORY_ALLOCATED, inventoryAllocated.getOrderId(), inventoryAllocated);

        return kafkaRequest.flatMap(step_ok -> Mono.just(true));
    }

}
