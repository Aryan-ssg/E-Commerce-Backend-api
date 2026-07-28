package com.example.Ecommerce.Common;

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
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
    

