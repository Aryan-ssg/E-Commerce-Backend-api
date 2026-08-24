package com.example.Ecommerce.Order.DTOs.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlaceOrderRequest {

    @NotEmpty(message = "Order items are required")
    @Valid
    private List<OrderItemsRequest> orderItems;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
}
