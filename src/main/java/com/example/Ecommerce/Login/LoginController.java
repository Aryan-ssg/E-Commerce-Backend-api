package com.example.Ecommerce.Login;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ecommerce.Common.JwtUtils;
import com.example.Ecommerce.Login.DTOs.LoginRequest;
import com.example.Ecommerce.Login.DTOs.LoginResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class LoginController {

    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;

    public LoginController(AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/public/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UsernamePasswordAuthenticationToken requestToken = new UsernamePasswordAuthenticationToken(request.getUserName(),
                request.getPassword());
        Authentication authentication = authenticationManager.authenticate(requestToken);

        UserDetails user = (UserDetails) authentication.getPrincipal();
       
       
        String token=jwtUtils.generateToken(user);

        LoginResponse response=new LoginResponse(token);
        return ResponseEntity.status(HttpStatus.OK).body(response);
        
        
    }

}
