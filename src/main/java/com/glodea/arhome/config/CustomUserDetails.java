package com.glodea.arhome.config;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomUserDetails implements UserDetails {

    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final String category;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(String email, String passwordHash, String fullName, String category, Collection<? extends GrantedAuthority> authorities) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.category = category;
        this.authorities = authorities;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
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
