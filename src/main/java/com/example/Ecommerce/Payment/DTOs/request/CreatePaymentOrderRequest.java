package com.example.Ecommerce.Payment.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentOrderRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Currency is required")
    private String currency = "INR";
}