package com.example.Ecommerce.Product.DTOs.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest {
    @NotBlank(message = "Product name is required")
    private String productName;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive ")
    private Integer productPrice;

    private String imageUrl;
}
