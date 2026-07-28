package com.example.Ecommerce.Order.DTOs.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemsResponse {

    private int quantity = 1;

    private Long productId;

    private int priceAtCheckout;

}
