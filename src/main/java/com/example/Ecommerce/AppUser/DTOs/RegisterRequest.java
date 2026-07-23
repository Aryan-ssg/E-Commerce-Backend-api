package com.example.Ecommerce.AppUser.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message="Username is required")
    private String userName;
    @NotBlank(message="Password is required")
    private String password;
    
}
