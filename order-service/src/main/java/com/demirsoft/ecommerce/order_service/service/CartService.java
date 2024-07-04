package com.demirsoft.ecommerce.order_service.service;

import java.util.LinkedList;
import java.util.Optional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demirsoft.ecommerce.order_service.entity.Cart;
import com.demirsoft.ecommerce.order_service.entity.OrderItem;
import com.demirsoft.ecommerce.order_service.exception.CartNotFoundException;
import com.demirsoft.ecommerce.order_service.repository.CartRepository;

@Service
@EnableKafka
@Transactional
public class CartService {
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    @Qualifier("OverwriteCartWithoutId")
    public ModelMapper modelMapperForOverwriteCartWithoutId;

    public Cart getCartOrCreate(Long customerId) {

        Optional<Cart> dbCart = cartRepository.findByCustomerId(customerId).stream().findFirst();

        return dbCart.orElseGet(() -> cartRepository.save(createAnEmptyCart(customerId)));
    }

    public Cart updateCart(Cart newCart) throws CartNotFoundException {

        Cart dbCart = cartRepository.findByCustomerId(newCart.getCustomerId()).stream().findFirst()
                .orElseThrow(() -> new CartNotFoundException(
                        String.format("customer id: %d", newCart.getCustomerId())));

        modelMapperForOverwriteCartWithoutId.map(newCart, dbCart);

        return cartRepository.save(dbCart);
    }

    public void clearCart(Long customerId) {

        Optional<Cart> dbCart = cartRepository.findByCustomerId(customerId).stream().findFirst();

        if (dbCart.isEmpty())
            return;

        dbCart.get().getItems().clear();

        cartRepository.save(dbCart.get());
    }

    public void deleteByCustomerId(Long customerId) {
        cartRepository.deleteByCustomerId(customerId);
    }

    private Cart createAnEmptyCart(Long customerId) {
        return new Cart(null, customerId, new LinkedList<OrderItem>() {
        });
    }

}
