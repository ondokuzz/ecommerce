package com.demirsoft.ecommerce.product_service.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.demirsoft.ecommerce.product_service.entity.Product;
import com.demirsoft.ecommerce.product_service.event.ProductDto;

@Configuration
public class ProductServiceConfig {

    @Bean
    @Qualifier("ProductDtoToProduct")
    public ModelMapper modelMapperForProductDtoToProduct() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.addMappings(new PropertyMap<ProductDto, Product>() {
            protected void configure() {

                skip(destination.getId());
                map().setName(source.getName());
                map().setBrand(source.getBrand());
                map().setDescription(source.getDescription());
                map().setPrice(source.getPrice());
                map().setQuantity(source.getQuantity());
            }
        });

        return modelMapper;
    }

    @Bean
    @Qualifier("OverwriteProductWithoutId")
    public ModelMapper modelMapperForOverwriteProductWithoutId() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.addMappings(new PropertyMap<Product, Product>() {
            protected void configure() {
                skip(destination.getId());
                map().setName(source.getName());
                map().setBrand(source.getBrand());
                map().setDescription(source.getDescription());
                map().setPrice(source.getPrice());
                map().setQuantity(source.getQuantity());
            }
        });

        return modelMapper;
    }

}
