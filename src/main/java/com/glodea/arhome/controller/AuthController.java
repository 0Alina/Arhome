package com.glodea.arhome.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.glodea.arhome.dto.RegisterRequest;
import com.glodea.arhome.service.RegistrationService;
import com.glodea.arhome.service.UserService;

@Controller
public class AuthController {

    private final RegistrationService registrationService;
    private final UserService userService;

    public AuthController(RegistrationService registrationService, UserService userService) {
        this.registrationService = registrationService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterRequest requestDto, HttpServletRequest request) {
        try {
            registrationService.register(
                requestDto.getFullName(),
                requestDto.getEmail(),
                requestDto.getPassword(),
                requestDto.getConfirmPassword()
            );
            UserDetails userDetails = userService.loadUserByUsername(requestDto.getEmail().trim().toLowerCase());
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
            return "redirect:/profile";
        } catch (IllegalArgumentException ex) {
            return "redirect:/?registerError";
        }
    }
}
