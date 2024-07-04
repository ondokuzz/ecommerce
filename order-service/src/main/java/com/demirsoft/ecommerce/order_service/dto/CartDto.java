package com.demirsoft.ecommerce.order_service.dto;

import java.util.List;

import com.demirsoft.ecommerce.order_service.entity.OrderItem;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDto {

    @NotNull(message = "Must not be NULL")
    private Long customerId;

    @NotNull(message = "Must not be NULL")
    private List<OrderItem> items;
}
