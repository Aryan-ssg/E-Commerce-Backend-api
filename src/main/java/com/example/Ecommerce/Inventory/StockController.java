package com.example.Ecommerce.Inventory;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ecommerce.Inventory.DTOs.request.StockRequest;
import com.example.Ecommerce.Product.DTOs.response.ProductAdminResponse;


import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class StockController {

    private StockService stockService;
    

    public StockController(StockService stockService) {
        this.stockService = stockService;
        
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/products/{productId}/restock")
    public ResponseEntity<ProductAdminResponse> reStock(@PathVariable Long productId, @Valid @RequestBody StockRequest request) {

          ProductAdminResponse response =stockService.reStock(productId, request.getStock());

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
