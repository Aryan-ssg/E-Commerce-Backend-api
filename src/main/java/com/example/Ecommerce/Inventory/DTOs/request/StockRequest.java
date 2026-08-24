package com.example.Ecommerce.Inventory.DTOs.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockRequest {

    @NotNull(message="Stock is required")
    @PositiveOrZero(message="Stock cannot be negative")
    private Integer stock;
    
}
