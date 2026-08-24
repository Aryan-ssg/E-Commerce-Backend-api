package com.example.Ecommerce.Login;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.AppUser.AppUserRepository;
import com.example.Ecommerce.Common.JwtUtils;
import com.example.Ecommerce.Common.RefreshTokenService;
import com.example.Ecommerce.Login.DTOs.LoginRequest;
import com.example.Ecommerce.Login.DTOs.LoginResponse;
import com.example.Ecommerce.Login.DTOs.RefreshRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class LoginController {

    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private RefreshTokenService refreshTokenService;
    private AppUserRepository appUserRepository;

    public LoginController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,
            RefreshTokenService refreshTokenService, AppUserRepository appUserRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping("/public/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UsernamePasswordAuthenticationToken requestToken = new UsernamePasswordAuthenticationToken(request.getUsername(),
                request.getPassword());
        Authentication authentication = authenticationManager.authenticate(requestToken);

        UserDetails user = (UserDetails) authentication.getPrincipal();

        AppUser appUser = appUserRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtUtils.generateAccessToken(user, appUser.getUserId(),
                "ROLE_" + appUser.getRole().name(), appUser.getTokenVersion());
        String refreshToken = jwtUtils.generateRefreshToken(user);

        refreshTokenService.storeRefreshToken(appUser, refreshToken);

        LoginResponse response = new LoginResponse(accessToken, refreshToken);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/public/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        RefreshTokenService.InvalidRefreshTokenException invalidEx = new RefreshTokenService.InvalidRefreshTokenException("Invalid refresh token");
        try {
            var storedOpt = refreshTokenService.validateAndRotate(refreshToken);
            if (storedOpt.isEmpty()) {
                throw invalidEx;
            }
            var stored = storedOpt.get();

            AppUser appUser = stored.getUser();

            // Generate new tokens
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(appUser.getUsername())
                    .password(appUser.getPassword())
                    .authorities("ROLE_" + appUser.getRole().name())
                    .build();

            String newAccessToken = jwtUtils.generateAccessToken(userDetails, appUser.getUserId(),
                    "ROLE_" + appUser.getRole().name(), appUser.getTokenVersion());
            String newRefreshToken = jwtUtils.generateRefreshToken(userDetails);

            refreshTokenService.rotateToken(stored, newRefreshToken);

            return ResponseEntity.ok(new LoginResponse(newAccessToken, newRefreshToken));

        } catch (RefreshTokenService.InvalidRefreshTokenException e) {
            throw invalidEx;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // For now, we'll just return success
        // In a real implementation, you might want to revoke the current access token's jti
        // For that, you'd need to extract jti from the Authorization header
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}