package com.example.Ecommerce.Common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.Ecommerce.AppUser.AppUser;
import com.example.Ecommerce.AppUser.AppUserRepository;


@Component
public class AuthenticationHelper {

    private final AppUserRepository appUserRepository;

    public AuthenticationHelper(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof JwtUserPrincipal)) {
            throw new UsernameNotFoundException("No authenticated user found");
        }

        JwtUserPrincipal principal = (JwtUserPrincipal) auth.getPrincipal();
        Long userId = principal.getUserId();

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));

        Long tokenVersion = principal.getTokenVersion();
        if (tokenVersion != null && !tokenVersion.equals(user.getTokenVersion())) {
            throw new UsernameNotFoundException("Token version mismatch - please login again");
        }

        return user;
    }

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof JwtUserPrincipal)) {
            return null;
        }

        return ((JwtUserPrincipal) auth.getPrincipal()).getUserId();
    }

    public Long getCurrentTokenVersion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof JwtUserPrincipal)) {
            return null;
        }

        return ((JwtUserPrincipal) auth.getPrincipal()).getTokenVersion();
    }
}