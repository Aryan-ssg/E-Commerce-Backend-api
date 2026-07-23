package com.example.Ecommerce.Order.DTOs.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PlaceOrderRequest {
    
    @NotNull(message="Order items are required")
   
    private List<OrderItemsRequest> orderItems;
    

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
}
