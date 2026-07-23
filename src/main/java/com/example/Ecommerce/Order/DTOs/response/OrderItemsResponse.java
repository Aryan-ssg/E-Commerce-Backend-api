package com.example.Ecommerce.Order.DTOs.response;

import com.example.Ecommerce.Product.Product;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.Positive;
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
