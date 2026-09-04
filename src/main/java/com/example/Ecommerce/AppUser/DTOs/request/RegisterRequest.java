package com.example.Ecommerce.AppUser.DTOs.request;

import com.example.Ecommerce.Common.Validation.FairPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    private String username;
    @NotBlank(message="Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @FairPassword
    private String password;
    
}
