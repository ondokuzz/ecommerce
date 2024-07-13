package com.demirsoft.ecommerce.order_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import com.demirsoft.ecommerce.order_service.entity.Address;
import com.demirsoft.ecommerce.order_service.entity.Cart;
import com.demirsoft.ecommerce.order_service.entity.Order;
import com.demirsoft.ecommerce.order_service.entity.OrderItem;
import com.demirsoft.ecommerce.order_service.entity.OrderStatus;
import com.demirsoft.ecommerce.order_service.event.OrderCreated;
import com.demirsoft.ecommerce.order_service.exception.CartNotFoundException;
import com.demirsoft.ecommerce.order_service.exception.OrderEmptyException;
import com.demirsoft.ecommerce.order_service.exception.OrderNotFoundException;
import com.demirsoft.ecommerce.order_service.repository.OrderRepository;
import com.demirsoft.ecommerce.order_service.service.CartService;
import com.demirsoft.ecommerce.order_service.service.OrderService;

@SpringBootTest
class OrderServiceTest {

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private CartService cartService;

    @MockBean
    private KafkaTemplate<String, OrderCreated> kafkaTemplate;

    @Autowired
    private OrderService orderService;

    private Order createFullOrder() {
        var order = new Order();
        order.setId("1");
        order.setCustomerId(1L);
        order.setCreditCardInfo("1234321");
        order.setShippingAddress(new Address("turkiye", "istanbul", "kosuyolu"));
        order.setPrice(23.0);
        return order;
    }

    private Cart createFullCart() {
        var cart = new Cart();

        var items = new LinkedList<OrderItem>();
        OrderItem item1 = new OrderItem();
        item1.setProductId("100");
        item1.setPrice(11.0);
        OrderItem item2 = new OrderItem();
        item2.setProductId("100");
        item2.setPrice(12.0);
        items = new LinkedList<OrderItem>();
        items.add(item1);
        items.add(item2);

        cart = new Cart();
        cart.setCustomerId(1L);
        cart.setItems(items);
        cart.setId("1");

        return cart;
    }

    @Test
    void givenCustomerId_whenGetCartOrCreateCalledTwice_thenReturnSameCart() {
        var cart = createFullCart();
        when(cartService.getCartOrCreate(1L)).thenReturn(cart);
        Cart result1 = orderService.getCartOrCreate(1L);
        Cart result2 = orderService.getCartOrCreate(1L);
        assertEquals(result1, cart);
        assertEquals(result1, result2);
    }

    @Test
    void givenACart_whenUpdateCart_thenReturnUpdatedCart() throws CartNotFoundException {
        var cart = createFullCart();
        when(cartService.updateCart(cart)).thenReturn(cart);
        Cart result = orderService.updateCart(cart);
        assertEquals(cart, result);
    }

    @Test
    void givenAnEmptyCart_whenCreateOrder_thenThrowException() throws OrderEmptyException {
        var cart = createFullCart();
        cart.getItems().clear();
        var order = createFullOrder();
        when(cartService.getCartOrCreate(1L)).thenReturn(cart);

        assertThrows(OrderEmptyException.class, () -> orderService.createOrder(order));
    }

    @Test
    void givenValidOrder_whenCreateOrder_thenTakeContentsFromCartAndClearCartAndPublishEvent()
            throws OrderEmptyException {
        var cart = createFullCart();
        var order = createFullOrder();
        when(cartService.getCartOrCreate(1L)).thenReturn(cart);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.createOrder(order);
        assertEquals(result.getStatus(), OrderStatus.CREATED.name());
        assertEquals(result.getItems(), cart.getItems());
        assertEquals(result.getPrice(), 23.0);

        OrderCreated event = createOrderCreated(order, cart);
        verify(cartService).clearCart(1L);
        verify(kafkaTemplate).send(OrderCreated.class.getSimpleName(), event.getId(), event);
    }

    @Test
    void givenOrderId_whenFinOrderById_thenReturnOrderWithThatId() throws OrderNotFoundException {
        var order = createFullOrder();
        when(orderRepository.findById("1")).thenReturn(Optional.of(order));
        Order result = orderService.findOrderById("1");
        assertEquals(order.getId(), "1");
        assertEquals(order, result);
    }

    @Test
    void givenOrderId_whenNoMatchFound_thenThrowException() {
        when(orderRepository.findById("1")).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.findOrderById("1"));
    }

    @Test
    void givenCustomerId_whenFindOrdersByCustomerId_thenReturnOrders() {
        Order order1 = createFullOrder();
        order1.setId("1");
        order1.setCustomerId(1L);
        Order order2 = createFullOrder();
        order2.setId("2");
        order2.setCustomerId(1L);
        when(orderRepository.findByCustomerId(1L)).thenReturn(Arrays.asList(order1, order2));
        List<Order> results = orderService.findOrdersByCustomerId(1L);
        assertEquals(2, results.size());
        assertEquals(results.get(0), order1);
        assertEquals(results.get(1), order2);
    }

    OrderCreated createOrderCreated(Order order, Cart cart) {
        OrderCreated orderCreated = new OrderCreated(
                order.getId(),
                cart.getCustomerId(),
                cart.getItems().stream().toList(),
                cart.getItems().stream().mapToDouble(OrderItem::getPrice).sum(),
                order.getCreditCardInfo(),
                order.getShippingAddress());
        return orderCreated;
    }
}
