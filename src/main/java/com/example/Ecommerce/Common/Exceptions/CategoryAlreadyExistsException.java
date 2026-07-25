package com.example.Ecommerce.Common.Exceptions;

public class CategoryAlreadyExistsException extends RuntimeException {

    public CategoryAlreadyExistsException(String categoryName){
        super(categoryName+" : Category already exists");
    }
    
}
