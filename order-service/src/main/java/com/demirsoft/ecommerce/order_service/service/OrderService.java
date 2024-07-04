package com.demirsoft.ecommerce.order_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demirsoft.ecommerce.order_service.entity.Cart;
import com.demirsoft.ecommerce.order_service.entity.Order;
import com.demirsoft.ecommerce.order_service.entity.OrderItem;
import com.demirsoft.ecommerce.order_service.entity.OrderStatus;
import com.demirsoft.ecommerce.order_service.exception.CartNotFoundException;
import com.demirsoft.ecommerce.order_service.exception.OrderEmptyException;
import com.demirsoft.ecommerce.order_service.exception.OrderNotFoundException;
import com.demirsoft.ecommerce.order_service.repository.OrderRepository;

@Service
@EnableKafka
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;

    private static final String ORDER_CREATED = "order_created";

    public Cart getCartOrCreate(Long customerId) {
        return cartService.getCartOrCreate(customerId);
    }

    public Cart updateCart(Cart newCart) throws CartNotFoundException {
        return cartService.updateCart(newCart);
    }

    @Transactional
    public Order createOrder(Order order) throws OrderEmptyException {

        Cart customerCart = cartService.getCartOrCreate(order.getCustomerId());

        if (customerCart.getItems().isEmpty())
            throw new OrderEmptyException(
                    String.format("order id: %s, customer id: %d", order.getId(), order.getCustomerId()));

        order.setItems(customerCart.getItems());
        order.setStatus(OrderStatus.CREATED.name());
        order.setPrice(calculatePrice(customerCart.getItems()));
        Order savedOrder = orderRepository.save(order);

        cartService.clearCart(order.getCustomerId());

        kafkaTemplate.send(ORDER_CREATED, order.getId(), savedOrder);

        return savedOrder;
    }

    private Double calculatePrice(List<OrderItem> items) {
        return items.stream().map(OrderItem::getPrice).reduce(0.0, Double::sum);
    }

    public Order findOrderById(String id) throws OrderNotFoundException {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> findOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    // @KafkaListener(topics = ORDER_CREATED, groupId =
    // KafkaConsumerConfig.CONSUMER_GROUP, autoStartup = "true")
    // public void consume(Order order, Acknowledgment ack) {
    // log.info("order topic received: " + order.toString());

    // ack.acknowledge();
    // }

}
