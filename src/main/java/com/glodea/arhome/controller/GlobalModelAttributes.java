package com.glodea.arhome.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.glodea.arhome.config.CustomUserDetails;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.UserRepository;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserRepository userRepository;

    public GlobalModelAttributes(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("navUserName")
    public String navUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = resolveEmail(authentication);
        if (email == null || email.isBlank()) {
            return null;
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.isRestricted()) {
            return null;
        }
        return user.getFullName();
    }

    @ModelAttribute("currentUserId")
    public Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = resolveEmail(authentication);
        if (email == null || email.isBlank()) {
            return null;
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.isRestricted()) {
            return null;
        }
        return user.getId();
    }

    private String resolveEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUsername();
        }
        if (principal instanceof OAuth2User) {
            Object email = ((OAuth2User) principal).getAttribute("email");
            if (email != null) {
                return email.toString().trim().toLowerCase();
            }
        }
        String fallback = authentication.getName();
        return fallback != null ? fallback.trim().toLowerCase() : null;
    }
}
