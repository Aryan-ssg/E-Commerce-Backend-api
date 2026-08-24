package com.example.Ecommerce.Common;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class JwtUserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final Long tokenVersion;
    private final Collection<? extends GrantedAuthority> authorities;

    public JwtUserPrincipal(Long userId, String username, Long tokenVersion,
            Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.tokenVersion = tokenVersion;
        this.authorities = authorities;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}