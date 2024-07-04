package com.demirsoft.ecommerce.product_service.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.demirsoft.ecommerce.product_service.entity.Product;
import com.demirsoft.ecommerce.product_service.event.ProductDto;
import com.demirsoft.ecommerce.product_service.service.ProductService;

import reactor.core.publisher.Mono;

@RestController
public class ProductController {

    @Autowired
    ProductService productService;

    @Autowired
    @Qualifier("ProductDtoToProduct")
    ModelMapper modelMapperProductDtoToProduct;

    @PostMapping("/products")
    public Mono<Product> createProduct(@RequestBody ProductDto productDto) {
        Product product = modelMapperProductDtoToProduct.map(productDto, Product.class);

        return productService.createProduct(product);
    }

    @PutMapping("/products")
    public Mono<Product> updateProduct(@RequestBody Product product) {
        return productService.updateProduct(product);
    }

    @GetMapping("/products/{id}")
    public Mono<Product> getProduct(@PathVariable String id) {
        return productService.findProductById(id);
    }

}