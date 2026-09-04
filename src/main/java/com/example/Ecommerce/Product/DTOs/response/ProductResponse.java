package com.example.Ecommerce.Product.DTOs.response;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {

    private Long productId;

    private String productName;

    private int productPrice;

    private String categoryName;

    private String imageUrl;

    private int stock;

   



}
