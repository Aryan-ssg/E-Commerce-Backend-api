package com.example.Ecommerce.Common.Validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = FairPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface FairPassword {
    String message() default "Password must be at least 'Fair' strength (8+ characters with at least 2 of: lowercase, uppercase, number, special character)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
