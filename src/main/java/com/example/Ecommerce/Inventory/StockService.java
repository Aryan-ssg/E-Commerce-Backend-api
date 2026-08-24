package com.example.Ecommerce.Inventory;

import com.example.Ecommerce.Product.DTOs.response.ProductAdminResponse;


public interface StockService {

    void reserveStock(Long productId,int quantity);

    void releaseStock(Long productId,int quantity);

    ProductAdminResponse reStock(Long productId,int quantity);
    
}
