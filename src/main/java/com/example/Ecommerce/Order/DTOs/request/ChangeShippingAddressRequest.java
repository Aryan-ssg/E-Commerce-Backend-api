package com.example.Ecommerce.Order.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChangeShippingAddressRequest {

    @NotBlank(message = "New shipping address is required")
    private String newShippingAddress;
    
}
