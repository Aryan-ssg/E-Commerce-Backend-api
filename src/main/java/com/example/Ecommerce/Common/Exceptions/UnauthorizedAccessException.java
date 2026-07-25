package com.example.Ecommerce.Common.Exceptions;

public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException(String message){
        super(message);
    }
    
}
