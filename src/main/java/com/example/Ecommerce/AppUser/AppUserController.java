package com.example.Ecommerce.AppUser;

import org.springframework.web.bind.annotation.RestController;

import com.example.Ecommerce.AppUser.DTOs.RegisterRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;


@RestController
@RequestMapping("/api")
public class AppUserController {

    private AppUserService userService;

    public AppUserController(AppUserService userService) {
        this.userService = userService;
    }

    
    @PostMapping("/public/register")
    public ResponseEntity<String> userRegistration(@Valid @RequestBody RegisterRequest request) {

        userService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully ");

    }


    






}
