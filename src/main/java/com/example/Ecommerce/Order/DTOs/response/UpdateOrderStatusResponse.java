package com.example.Ecommerce.Order.DTOs.response;

import com.example.Ecommerce.Order.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusResponse {

    private Long orderId;
    private OrderStatus orderStatus;
    
}
