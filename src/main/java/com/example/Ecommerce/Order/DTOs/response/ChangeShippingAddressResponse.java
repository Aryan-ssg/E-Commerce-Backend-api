package com.example.Ecommerce.Order.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeShippingAddressResponse {
    private Long orderId;

    private String addressLine;

    private String pinCode;

    private String landmark;

    private String contactNumber;

}
