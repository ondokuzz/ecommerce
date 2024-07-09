package com.demirsoft.ecommerce.order_service.dto;

import com.demirsoft.ecommerce.order_service.entity.Address;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    @NotNull(message = "Must not be NULL")
    private Long customerId;

    @NotNull(message = "Must not be NULL")
    private Address shippingAddress;

    @NotEmpty(message = "Must not be Empty or NULL")
    private String creditCardInfo;

}
