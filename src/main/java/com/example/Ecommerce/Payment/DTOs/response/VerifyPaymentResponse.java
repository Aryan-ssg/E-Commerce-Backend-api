package com.example.Ecommerce.Payment.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPaymentResponse {
    private boolean success;
    private String message;
    private String orderId;
}