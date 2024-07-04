package com.demirsoft.ecommerce.order_service.dto;

import com.demirsoft.ecommerce.order_service.entity.Address;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderDto {

    @NotNull(message = "Must not be NULL")
    private Long customerId;

    @NotEmpty(message = "Must not be Empty or NULL")
    private Address shippingAddress;

    @NotEmpty(message = "Must not be Empty or NULL")
    private String creditCardInfo;

}
