package com.example.Ecommerce.Common.Exceptions;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(){
        super("Username already exists");
    }
    
}
