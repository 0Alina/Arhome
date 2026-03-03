package com.glodea.arhome.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.glodea.arhome.dto.CategoryUpdateRequest;
import com.glodea.arhome.dto.RecipeDto;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.UserRepository;
import com.glodea.arhome.service.RecipeService;
import java.util.List;

import com.glodea.arhome.config.CustomUserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final RecipeService recipeService;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository, RecipeService recipeService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.recipeService = recipeService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        String email = resolveEmail(SecurityContextHolder.getContext().getAuthentication());
        if (email == null || email.isBlank()) {
            return "redirect:/?authError#auth-login";
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = ensureUserForOAuth(authentication, email);
            if (user == null) {
                return "redirect:/?authError#auth-login";
            }
        }
        List<RecipeDto> recipes = recipeService.getRecipesForUser(user);
        model.addAttribute("recipes", recipes);
        model.addAttribute("profileFullName", user.getFullName());
        model.addAttribute("profileEmail", user.getEmail());
        model.addAttribute("profileCategory", user.getCategory());
        model.addAttribute("profileAverageRating", recipeService.getAverageRatingForUser(user));
        String category = normalizeCategory(user.getCategory());
        if (category != null && !category.equals(user.getCategory())) {
            user.setCategory(category);
            userRepository.save(user);
        }
        boolean needsCategory = category == null || category.isBlank() || "Unselected".equals(category);
        model.addAttribute("needsCategory", needsCategory);
        return "profile";
    }

    @PostMapping("/profile/category")
    public String updateCategory(@ModelAttribute CategoryUpdateRequest requestDto, HttpServletRequest request) {
        String category = requestDto.getCategory();
        String email = resolveEmail(SecurityContextHolder.getContext().getAuthentication());
        if (email == null || email.isBlank()) {
            return "redirect:/?authError#auth-login";
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = ensureUserForOAuth(authentication, email);
            if (user == null) {
                return "redirect:/?authError#auth-login";
            }
        }
        user.setCategory(category);
        userRepository.save(user);

        String role = user.getRole();
        UserDetails userDetails = new CustomUserDetails(
            user.getEmail(),
            user.getPasswordHash(),
            user.getFullName(),
            user.getCategory(),
            List.of(() -> "ROLE_" + role)
        );

        UsernamePasswordAuthenticationToken authenticationToken =
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext());

        return "redirect:/profile";
    }

    private String resolveEmail(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
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

    private User ensureUserForOAuth(Authentication authentication, String email) {
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof OAuth2User)) {
            return null;
        }
        OAuth2User oauth2User = (OAuth2User) principal;
        String fullName = normalizeName(oauth2User.getAttribute("name"));
        if (fullName == null || fullName.isBlank()) {
            String givenName = normalizeName(oauth2User.getAttribute("given_name"));
            String familyName = normalizeName(oauth2User.getAttribute("family_name"));
            if (givenName != null && familyName != null) {
                fullName = (givenName + " " + familyName).trim();
            } else if (givenName != null) {
                fullName = givenName;
            } else if (familyName != null) {
                fullName = familyName;
            }
        }
        if (fullName == null || fullName.isBlank()) {
            fullName = email;
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setRole("USER");
        user.setCategory("Unselected");
        return userRepository.save(user);
    }

    private String normalizeName(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return null;
        }
        String trimmed = category.trim();
        switch (trimmed) {
            case "Teen Kitchen (13-19)":
                return "Teen Kitchen (13-19 years)";
            case "Young & Hungry (20-35)":
                return "Young & Hungry (20-35 years)";
            case "Home Cooks (36-55)":
                return "Home Cooks (36-55 years)";
            case "Grandma’s Classics (56+)":
            case "Grandma's Classics (56+)":
                return "Grandma’s Classics (56+ years)";
            default:
                return trimmed;
        }
    }
}
