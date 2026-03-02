package com.glodea.arhome.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.glodea.arhome.service.RecipeService;
import com.glodea.arhome.repository.UserRepository;
import com.glodea.arhome.entity.User;

@Controller
public class HomeController {

    private final RecipeService recipeService;
    private final UserRepository userRepository;

    public HomeController(RecipeService recipeService, UserRepository userRepository) {
        this.recipeService = recipeService;
        this.userRepository = userRepository;
    }

    @GetMapping({"/", "/home"})
    public String home() {
        return "index";
    }

    @GetMapping("/recipes")
    public String recipes(@RequestParam(value = "q", required = false) String query,
                          @RequestParam(value = "mealType", required = false) java.util.List<String> mealTypes,
                          @RequestParam(value = "region", required = false) java.util.List<String> regions,
                          @RequestParam(value = "by", required = false) java.util.List<String> byCategories,
                          @RequestParam(value = "time", required = false) java.util.List<String> timeRanges,
                          @RequestParam(value = "style", required = false) java.util.List<String> styles,
                          @RequestParam(value = "nutrition", required = false) java.util.List<String> nutritions,
                          Model model) {
        java.util.List<String> selectedMealTypes = sanitizeFilterValues(mealTypes);
        java.util.List<String> selectedRegions = sanitizeFilterValues(regions);
        java.util.List<String> selectedByCategories = sanitizeFilterValues(byCategories);
        java.util.List<String> selectedTimeRanges = sanitizeFilterValues(timeRanges);
        java.util.List<String> selectedStyles = sanitizeFilterValues(styles);
        java.util.List<String> selectedNutritions = sanitizeFilterValues(nutritions);

        model.addAttribute("recipes", recipeService.searchRecipes(
            query,
            selectedMealTypes,
            selectedRegions,
            selectedByCategories,
            selectedTimeRanges,
            selectedStyles,
            selectedNutritions
        ));
        model.addAttribute("searchQuery", query == null ? "" : query.trim());
        model.addAttribute("selectedMealTypes", selectedMealTypes);
        model.addAttribute("selectedRegions", selectedRegions);
        model.addAttribute("selectedByCategories", selectedByCategories);
        model.addAttribute("selectedTimeRanges", selectedTimeRanges);
        model.addAttribute("selectedStyles", selectedStyles);
        model.addAttribute("selectedNutritions", selectedNutritions);
        User user = getCurrentUser();
        if (user != null) {
            model.addAttribute("favoriteRecipeIds", recipeService.getFavoriteRecipeIds(user));
        }
        return "recipes";
    }

    @GetMapping("/plans")
    public String plans() {
        return "plans";
    }

    @GetMapping({"/groceries", "/search"})
    public String groceries() {
        return "groceries";
    }

    @GetMapping("/collection")
    public String collection(Model model) {
        User user = getCurrentUser();
        if (user != null) {
            model.addAttribute("favoriteRecipes", recipeService.getFavoriteRecipesForUser(user));
            model.addAttribute("favoriteRecipeIds", recipeService.getFavoriteRecipeIds(user));
        } else {
            model.addAttribute("favoriteRecipes", java.util.List.of());
        }
        return "collection";
    }


    @GetMapping("/login")
    public String login() {
        return "login";
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email = resolveEmail(authentication);
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    private String resolveEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.glodea.arhome.config.CustomUserDetails) {
            return ((com.glodea.arhome.config.CustomUserDetails) principal).getUsername();
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

    private java.util.List<String> sanitizeFilterValues(java.util.List<String> values) {
        if (values == null || values.isEmpty()) {
            return java.util.List.of();
        }
        return values.stream()
            .filter(org.springframework.util.StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();
    }

}



