package com.example.Ecommerce.Payment.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentOrderResponse {
    private String razorpayOrderId;
    private int amount;
    private String currency;
    private String keyId;
    private String receipt;
}