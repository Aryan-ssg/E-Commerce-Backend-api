package com.example.Ecommerce.Category.DTOs.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Productsresponse {

    
    private Long productId;

  
    private String productName;

   
    private int productPrice;
}