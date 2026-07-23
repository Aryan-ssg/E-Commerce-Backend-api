package com.example.Ecommerce.Order.DTOs.request;

import com.example.Ecommerce.Order.OrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateOrderStatusRequest {
    
    @NotNull(message="Updated status is required")
    private OrderStatus updatedStatus;
}
