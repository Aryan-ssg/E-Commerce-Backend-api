package com.example.Ecommerce.Common;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.AppUser.AppUserRepository;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final AppUserRepository appUserRepository;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthFilter(JwtUtils jwtUtils, AppUserRepository appUserRepository,
            JwtAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtUtils = jwtUtils;
        this.appUserRepository = appUserRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtils.extractUsername(token);
            Long userId = jwtUtils.extractUserId(token);
            String role = jwtUtils.extractRole(token);
            Long tokenVersion = jwtUtils.extractTokenVersion(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Central version check: compare token's ver claim with DB
                AppUser user = appUserRepository.findById(userId)
                        .orElseThrow(() -> new TokenVersionMismatchException("User no longer exists"));

                Long tokenVersionValue = (tokenVersion == null) ? 0L : tokenVersion;
                Long storedVersion = (user.getTokenVersion() == null) ? 0L : user.getTokenVersion();
                if (!tokenVersionValue.equals(storedVersion)) {
                    throw new TokenVersionMismatchException("Token version mismatch - please login again");
                }

                // Validate signature & expiry (no DB hit)
                if (!jwtUtils.validateToken(token, username)) {
                    authenticationEntryPoint.commence(request, response,
                            new BadCredentialsException("Invalid or expired token"));
                    return;
                }

                // Build authorities from token claim (no DB lookup for role)
                var authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        new JwtUserPrincipal(userId, username, tokenVersion, authorities), null, authorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (JwtException | TokenVersionMismatchException e) {
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException(e.getMessage(), e));
            return;
        }

        filterChain.doFilter(request, response);
    }

    public static class TokenVersionMismatchException extends RuntimeException {
        public TokenVersionMismatchException(String message) {
            super(message);
        }
    }
}