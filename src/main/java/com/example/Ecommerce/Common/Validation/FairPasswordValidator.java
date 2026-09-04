package com.example.Ecommerce.Common.Validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FairPasswordValidator implements ConstraintValidator<FairPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) return true; // @NotBlank handles null/blank
        return PasswordStrength.isAtLeastFair(password);
    }
}
