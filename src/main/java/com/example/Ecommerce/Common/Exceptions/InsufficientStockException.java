package com.example.Ecommerce.Common.Exceptions;

public class InsufficientStockException  extends RuntimeException{

    public InsufficientStockException(String message){
        super(message);
    }
    
}
