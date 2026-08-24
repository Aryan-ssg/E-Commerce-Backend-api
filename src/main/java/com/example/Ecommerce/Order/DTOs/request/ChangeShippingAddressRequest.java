package com.example.Ecommerce.Order.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChangeShippingAddressRequest {

    @NotBlank(message = "Address is required")
    private String addressLine;

    @NotBlank(message = "PIN code is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Enter a valid 6-digit PIN code")
    private String pinCode;

    private String landmark;
}
