package com.demirsoft.ecommerce.product_service.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.kafka.common.protocol.types.Field.Bool;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demirsoft.ecommerce.product_service.config.KafkaConsumerConfig;
import com.demirsoft.ecommerce.product_service.entity.InventoryRequest;
import com.demirsoft.ecommerce.product_service.entity.InventoryRequestStatus;
import com.demirsoft.ecommerce.product_service.entity.Product;
import com.demirsoft.ecommerce.product_service.event.InventoryAllocated;
import com.demirsoft.ecommerce.product_service.event.OrderCreated;
import com.demirsoft.ecommerce.product_service.event.OrderFailed;
import com.demirsoft.ecommerce.product_service.event.OrderItem;
import com.demirsoft.ecommerce.product_service.exception.ProductNotFoundException;
import com.demirsoft.ecommerce.product_service.repository.InventoryRequestRepository;
import com.demirsoft.ecommerce.product_service.repository.ProductRepository;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Log4j2
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRequestRepository inventoryRequestRepository;

    @Autowired
    @Qualifier("OverwriteProductWithoutId")
    public ModelMapper modelMapperForOverwriteProductWithoutId;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

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

        getPreviousRequestsFoSameOrder(order)
                .collectList()
                .flatMapMany(requestHistory -> {
                    if (requestHistory.size() > 0) {
                        // this order has already been processed before
                        return Mono.empty();
                    }

                    return processRequest(order, ack, requestHistory);

                }).subscribe(
                        result -> log.debug("order topic processing successful: " + order.toString()),
                        error -> log.debug("order topic processing failed: " + order.toString()),
                        () -> ack.acknowledge());
    }

    public Mono<Boolean> processRequest(OrderCreated order, Acknowledgment ack, List<InventoryRequest> requestHistory) {

        List<String> productIds = extractProductIdList(order);

        Map<String, Integer> productIdToRequestedQuantityMap = extractProductIdQuantityMap(order);

        return productRepository.findAllById(productIds).collectList()
                .flatMapMany(products -> {

                    var missingItems = checkAndDecrementProductQuantities(
                            products, productIds,
                            productIdToRequestedQuantityMap);

                    if (missingItems.size() > 0) {
                        rejectOrder(order, missingItems);
                        return Mono.empty();
                    }

                    return acceptOrder(order, products);
                })
                .next();

    }

    private LinkedList<String> checkAndDecrementProductQuantities(
            List<Product> products,
            List<String> productIds,
            Map<String, Integer> productIdToRequestedQuantityMap) {

        var missingItems = new LinkedList<String>();

        products.forEach(product -> {
            if (productIdToRequestedQuantityMap.containsKey(product.getId())) {
                int requestedQuantity = productIdToRequestedQuantityMap.get(product.getId());
                if (product.getQuantity() < requestedQuantity) {
                    missingItems.add(String.format("item: %s is missing by amount %d", product.getId(),
                            requestedQuantity - product.getQuantity()));
                } else {
                    product.setQuantity(product.getQuantity() - requestedQuantity);
                }

                productIds.remove(product.getId());
            }
        });

        productIds.stream().forEach(productId -> {
            int requestedQuantity = productIdToRequestedQuantityMap.get(productId);
            missingItems.add(String.format("item: %s is missing by amount %d", productId, requestedQuantity));
        });

        return missingItems;
    }

    private List<String> extractProductIdList(OrderCreated order) {
        return order.getItems().stream()
                .map(OrderItem::getProductId)
                .toList();

    }

    private Map<String, Integer> extractProductIdQuantityMap(OrderCreated order) {
        return order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getProductId, OrderItem::getQuantity));

    }

    private Flux<InventoryRequest> getPreviousRequestsFoSameOrder(OrderCreated order) {
        return inventoryRequestRepository.findByOrderId(order.getId());
    }

    @Transactional
    private void rejectOrder(OrderCreated order, List<String> reason) {
        var orderFailed = new OrderFailed(order.getId(), order.getCustomerId(), reason);

        // setting null id creates a new log for this order id
        inventoryRequestRepository
                .save(new InventoryRequest(null, order.getId(), InventoryRequestStatus.ITEMS_UNAVAILABLE));

        kafkaTemplate.send(ORDER_FAILED, orderFailed.getId(), orderFailed);
    }

    @Transactional
    private Mono<Boolean> acceptOrder(OrderCreated order, List<Product> products) {
        // update product repo with new quantities
        AtomicBoolean productRepoSaved = new AtomicBoolean(false);
        AtomicBoolean inventoryRepoSaved = new AtomicBoolean(false);

        return updateProductRepo(products)
                .flatMap(prev_step_ok -> {
                    productRepoSaved.set(true);

                    return updateInventoryHistory(order);
                })
                .flatMap(prev_step_ok -> {
                    inventoryRepoSaved.set(true);

                    return sendInventoryAllocatedEvent(order);
                })
                .then(Mono.just(true))
                .doOnError(one_of_steps_failed -> {
                    // rollback successful steps,
                    if (productRepoSaved.get())
                        rollbackProductRepo(order, products);
                    if (inventoryRepoSaved.get())
                        rollbackInventoryRepo(order);
                    // kafka step is not processed or already failed
                })
                .then(Mono.just(false));
    }

    private Mono<Boolean> updateProductRepo(List<Product> products) {
        return productRepository.saveAll(products).collectList().flatMap(step_ok -> Mono.just(true));
    }

    private Mono<Boolean> updateInventoryHistory(OrderCreated order) {
        // setting null id creates a new log for this order id
        var inventoryRequest = new InventoryRequest(null, order.getId(), InventoryRequestStatus.ITEMS_ALLOCATED);

        return inventoryRequestRepository.save(inventoryRequest).flatMap(step_ok -> Mono.just(true));
    }

    private Mono<Boolean> sendInventoryAllocatedEvent(OrderCreated order) {

        var inventoryAllocated = new InventoryAllocated(
                order.getId(), order.getCustomerId(), order.getCreditCardInfo(),
                order.getPrice(), order.getShippingAddress());

        var kafkaFuture = kafkaTemplate.send(INVENTORY_ALLOCATED, inventoryAllocated.getOrderId(), inventoryAllocated);

        return Mono.fromFuture(kafkaFuture).flatMap(step_ok -> Mono.just(true));
    }

    private void rollbackInventoryRepo(OrderCreated order) {
        throw new UnsupportedOperationException("Unimplemented method 'rollbackInventoryRepo'");
    }

    private void rollbackProductRepo(OrderCreated order, List<Product> products) {
        throw new UnsupportedOperationException("Unimplemented method 'rollbackProductRepo'");
    }

}
