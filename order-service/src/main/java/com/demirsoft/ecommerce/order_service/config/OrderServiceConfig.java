package com.demirsoft.ecommerce.order_service.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.demirsoft.ecommerce.order_service.dto.CartDto;
import com.demirsoft.ecommerce.order_service.dto.OrderDto;
import com.demirsoft.ecommerce.order_service.entity.Cart;
import com.demirsoft.ecommerce.order_service.entity.Order;
import com.demirsoft.ecommerce.order_service.event.OrderCreated;

@Configuration
public class OrderServiceConfig {

    @Bean
    @Qualifier("OrderDtoToOrder")
    public ModelMapper modelMapperForOrderDtoToOrder() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.addMappings(new PropertyMap<OrderDto, Order>() {
            protected void configure() {

                skip(destination.getId());
                map().setCustomerId(source.getCustomerId());
                map().setCreditCardInfo(source.getCreditCardInfo());
                map(source.getShippingAddress().getState(), destination.getShippingAddress().getState());
                map(source.getShippingAddress().getCity(), destination.getShippingAddress().getCity());
                map(source.getShippingAddress().getStreet(), destination.getShippingAddress().getStreet());

            }
        });

        return modelMapper;
    }

    // private List<OrderItem> items;

    @Bean
    @Qualifier("OrderToOrderCreated")
    public ModelMapper modelMapperForOrderToOrderCreated() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.addMappings(new PropertyMap<Order, OrderCreated>() {
            protected void configure() {

                map().setId(source.getId());
                map().setCustomerId(source.getCustomerId());
                map().setCreditCardInfo(source.getCreditCardInfo());
                map().setPrice(source.getPrice());
                map(source.getShippingAddress().getState(), destination.getShippingAddress().getState());
                map(source.getShippingAddress().getCity(), destination.getShippingAddress().getCity());
                map(source.getShippingAddress().getStreet(), destination.getShippingAddress().getStreet());
                map().setItems(source.getItems());
            }
        });

        return modelMapper;
    }

    @Bean
    @Qualifier("CartDtoToCart")
    public ModelMapper modelMapperForCartDtoToCart() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.addMappings(new PropertyMap<CartDto, Cart>() {
            protected void configure() {
                skip(destination.getId());
                map().setCustomerId(source.getCustomerId());
                map().setItems(source.getItems());
            }
        });

        return modelMapper;
    }

    @Bean
    @Qualifier("OverwriteCartWithoutId")
    public ModelMapper modelMapperForOverwriteCartWithoutId() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.addMappings(new PropertyMap<Cart, Cart>() {
            protected void configure() {
                skip(destination.getId());
                map().setCustomerId(source.getCustomerId());
                map().setItems(source.getItems());
            }
        });

        return modelMapper;
    }

}
