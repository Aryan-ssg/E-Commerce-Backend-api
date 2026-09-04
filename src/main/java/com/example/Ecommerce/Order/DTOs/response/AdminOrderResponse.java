package com.example.Ecommerce.Order.DTOs.response;

import java.time.LocalDateTime;

import com.example.Ecommerce.Order.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderResponse {

    private Long orderId;

    private int totalPrice;

    private String username;

    private OrderStatus orderStatus;

    private LocalDateTime orderDateTime;

}
