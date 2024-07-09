package com.demirsoft.ecommerce.order_service.controller;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.demirsoft.ecommerce.order_service.dto.CartDto;
import com.demirsoft.ecommerce.order_service.dto.OrderDto;
import com.demirsoft.ecommerce.order_service.entity.Cart;
import com.demirsoft.ecommerce.order_service.entity.Order;
import com.demirsoft.ecommerce.order_service.exception.CartNotFoundException;
import com.demirsoft.ecommerce.order_service.exception.OrderEmptyException;
import com.demirsoft.ecommerce.order_service.exception.OrderNotFoundException;
import com.demirsoft.ecommerce.order_service.service.OrderService;

import jakarta.validation.Valid;

@RestController
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    @Qualifier("OrderDtoToOrder")
    ModelMapper modelMapperOrderDtoToOrder;

    @Autowired
    @Qualifier("CartDtoToCart")
    ModelMapper modelMapperCartDtoToCart;

    @GetMapping("/carts/{customerId}")
    public ResponseEntity<Cart> getCart(@PathVariable Long customerId) {
        Cart cart = orderService.getCartOrCreate(customerId);

        return ResponseEntity.ok().body(cart);
    }

    @PutMapping("/carts")
    public ResponseEntity<Cart> updateCart(@Valid @RequestBody CartDto newCart) throws CartNotFoundException {
        Cart cart = modelMapperCartDtoToCart.map(newCart, Cart.class);

        Cart updatedCart = orderService.updateCart(cart);

        return ResponseEntity.ok().body(updatedCart);

    }

    @PostMapping("/orders")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderDto orderRequest) throws OrderEmptyException {
        Order order = modelMapperOrderDtoToOrder.map(orderRequest, Order.class);

        Order createdOrder = orderService.createOrder(order);

        return ResponseEntity.ok().body(createdOrder);

    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable String id) throws OrderNotFoundException {
        Order order = orderService.findOrderById(id);

        return ResponseEntity.ok().body(order);

    }

    @GetMapping("/orders/customer/{customerId}")
    public ResponseEntity<List<Order>> getOrder(@PathVariable Long customerId) {
        var orders = orderService.findOrdersByCustomerId(customerId);

        return ResponseEntity.ok().body(orders);
    }

}
