package com.demirsoft.ecommerce.order_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.demirsoft.ecommerce.order_service.config.OrderServiceConfig;
import com.demirsoft.ecommerce.order_service.entity.Cart;
import com.demirsoft.ecommerce.order_service.entity.OrderItem;
import com.demirsoft.ecommerce.order_service.exception.CartNotFoundException;
import com.demirsoft.ecommerce.order_service.repository.CartRepository;
import com.demirsoft.ecommerce.order_service.service.CartService;

@ExtendWith(SpringExtension.class)
@Import(OrderServiceConfig.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    // @BeforeEach
    // public void setUp() {
    // MockitoAnnotations.openMocks(this);
    // }

    private Cart createfullCart(Long customerId) {
        OrderItem item1 = new OrderItem();
        item1.setProductId("100");
        item1.setPrice(11.0);

        OrderItem item2 = new OrderItem();
        item2.setProductId("100");
        item2.setPrice(12.0);

        var items = new LinkedList<OrderItem>();
        items.add(item1);
        items.add(item2);

        Cart newCart = new Cart("1", customerId, items);

        return newCart;
    }

    @Test
    public void givenCustomerId_whenGetCartOrCreateCalledTwice_thenReturnTheSameCart() {
        Long customerId = 1L;
        Cart existingCart = new Cart("1", customerId, new LinkedList<OrderItem>());
        when(cartRepository.findByCustomerId(customerId)).thenReturn(List.of(existingCart));

        Cart result1 = cartService.getCartOrCreate(customerId);
        Cart result2 = cartService.getCartOrCreate(customerId);

        assertEquals(existingCart, result1);
        assertEquals(result1, result2);
    }

    @Test
    public void givenCustomerId_whenNoCartFound_thenCreateANewOne() {
        Long customerId = 1L;
        Cart newCart = new Cart("1", customerId, new LinkedList<OrderItem>());
        when(cartRepository.findByCustomerId(customerId)).thenReturn(List.of());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        Cart result = cartService.getCartOrCreate(customerId);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(result.getCustomerId(), customerId);
        assertTrue(result.getItems().isEmpty());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    public void givenExistingCart_whenUpdateCart_thenCartUpdated() throws CartNotFoundException {
        Long customerId = 1L;

        OrderItem item1 = new OrderItem();
        item1.setProductId("100");
        item1.setPrice(11.0);
        OrderItem item2 = new OrderItem();
        item2.setProductId("100");
        item2.setPrice(12.0);
        var items = new LinkedList<OrderItem>();
        items.add(item1);
        items.add(item2);
        Cart newCart = new Cart("1", customerId, items);
        Cart existingCart = new Cart("1", customerId, new LinkedList<OrderItem>());

        when(cartRepository.findByCustomerId(customerId)).thenReturn(List.of(existingCart));

        Cart result = cartService.updateCart(newCart);
        assertEquals(newCart, result);

        verify(cartRepository, times(1)).save(newCart);
    }

    @Test
    public void givenCartDoesntExist_whenUpdateCart_thenCartUpdated() throws CartNotFoundException {
        Long customerId = 1L;
        Cart newCart = new Cart("1", customerId, new LinkedList<OrderItem>());
        when(cartRepository.findByCustomerId(customerId)).thenReturn(List.of());

        assertThrows(CartNotFoundException.class, () -> cartService.updateCart(newCart));
    }

    @Test
    public void givenExistingFullCart_whenClearCart_thenCartEmpty() throws CartNotFoundException {
        Long customerId = 1L;
        Cart existingCart = createfullCart(customerId);
        existingCart.getItems().add(new OrderItem());
        when(cartRepository.findByCustomerId(customerId)).thenReturn(List.of(existingCart));

        cartService.clearCart(customerId);

        assertTrue(existingCart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(existingCart);
    }

    @Test
    public void givenCartDoesntExist_whenClearCart_thenCartEmpty() throws CartNotFoundException {
        Long customerId = 1L;
        when(cartRepository.findByCustomerId(customerId)).thenReturn(List.of());

        cartService.clearCart(customerId);

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    public void testDeleteByCustomerId() {
        Long customerId = 1L;

        cartService.deleteByCustomerId(customerId);

        verify(cartRepository, times(1)).deleteByCustomerId(customerId);
    }
}
