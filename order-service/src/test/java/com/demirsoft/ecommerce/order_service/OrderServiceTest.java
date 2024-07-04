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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.demirsoft.ecommerce.order_service.entity.Cart;
import com.demirsoft.ecommerce.order_service.entity.Order;
import com.demirsoft.ecommerce.order_service.entity.OrderItem;
import com.demirsoft.ecommerce.order_service.entity.OrderStatus;
import com.demirsoft.ecommerce.order_service.exception.CartNotFoundException;
import com.demirsoft.ecommerce.order_service.exception.OrderEmptyException;
import com.demirsoft.ecommerce.order_service.exception.OrderNotFoundException;
import com.demirsoft.ecommerce.order_service.repository.OrderRepository;
import com.demirsoft.ecommerce.order_service.service.CartService;
import com.demirsoft.ecommerce.order_service.service.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartService cartService;

    @Mock
    private KafkaTemplate<String, Order> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    private Order order;
    private Cart cart;
    private List<OrderItem> items;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId("1");
        order.setCustomerId(1L);

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
        cart.setItems(items);
    }

    @Test
    void givenCustomerId_whenGetCartOrCreateCalledTwice_thenReturnSameCart() {
        when(cartService.getCartOrCreate(1L)).thenReturn(cart);
        Cart result1 = orderService.getCartOrCreate(1L);
        Cart result2 = orderService.getCartOrCreate(1L);
        assertEquals(result1, cart);
        assertEquals(result1, result2);
    }

    @Test
    void givenACart_whenUpdateCart_thenReturnUpdatedCart() throws CartNotFoundException {
        when(cartService.updateCart(cart)).thenReturn(cart);
        Cart result = orderService.updateCart(cart);
        assertEquals(cart, result);
    }

    @Test
    void givenAnEmptyCart_whenCreateOrder_thenThrowException() throws OrderEmptyException {
        cart.getItems().clear();
        when(cartService.getCartOrCreate(1L)).thenReturn(cart);

        assertThrows(OrderEmptyException.class, () -> orderService.createOrder(order));
    }

    @Test
    void givenValidOrder_whenCreateOrder_thenTakeContentsFromCartAndClearCartAndPublishEvent()
            throws OrderEmptyException {
        when(cartService.getCartOrCreate(1L)).thenReturn(cart);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        Order result = orderService.createOrder(order);
        assertEquals(result.getStatus(), OrderStatus.CREATED.name());
        assertEquals(result.getItems(), cart.getItems());
        assertEquals(result.getPrice(), 23.0);

        verify(cartService).clearCart(1L);
        verify(kafkaTemplate).send("order_created", "1", order);
    }

    @Test
    void givenOrderId_whenFinOrderById_thenReturnOrderWithThatId() throws OrderNotFoundException {
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
        Order order1 = new Order();
        order1.setId("1");
        order1.setCustomerId(1L);
        Order order2 = new Order();
        order2.setId("2");
        order2.setCustomerId(1L);
        when(orderRepository.findByCustomerId(1L)).thenReturn(Arrays.asList(order1, order2));
        List<Order> results = orderService.findOrdersByCustomerId(1L);
        assertEquals(2, results.size());
        assertEquals(results.get(0), order1);
        assertEquals(results.get(1), order2);
    }
}
