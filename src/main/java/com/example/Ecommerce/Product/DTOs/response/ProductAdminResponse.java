package com.example.Ecommerce.Product.DTOs.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductAdminResponse {
     private Long productId;

    private String productName;

    private int productPrice;

    private String categoryName;

   private Integer stock;

   private String imageUrl;
}
