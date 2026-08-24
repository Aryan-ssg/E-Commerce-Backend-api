package com.example.Ecommerce.AppUser;

import org.springframework.web.bind.annotation.RestController;

import com.example.Ecommerce.AppUser.DTOs.request.ChangePasswordRequest;
import com.example.Ecommerce.AppUser.DTOs.request.RegisterRequest;
import com.example.Ecommerce.AppUser.DTOs.response.UserAdminResponse;
import com.example.Ecommerce.Common.DTOs.PagedResponse;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @PostMapping("/user/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.status(HttpStatus.OK).body("Password changed successfully. Please login again.");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<PagedResponse<UserAdminResponse>> getAllUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 20, sort = "userId") Pageable pageable) {
        PagedResponse<UserAdminResponse> response = userService.getAllUsers(username, role, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
