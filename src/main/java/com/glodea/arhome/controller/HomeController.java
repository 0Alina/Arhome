package com.glodea.arhome.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.glodea.arhome.service.RecipeService;
import com.glodea.arhome.service.GroceryService;
import com.glodea.arhome.repository.UserRepository;
import com.glodea.arhome.entity.User;

@Controller
public class HomeController {

    private static final int RECIPES_MAX_ROWS = 4;
    private static final int RECIPES_DEFAULT_COLUMNS = 2;

    private final RecipeService recipeService;
    private final GroceryService groceryService;
    private final UserRepository userRepository;

    public HomeController(RecipeService recipeService, GroceryService groceryService, UserRepository userRepository) {
        this.recipeService = recipeService;
        this.groceryService = groceryService;
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
                          @RequestParam(value = "page", required = false) Integer page,
                          Model model) {
        java.util.List<String> selectedMealTypes = sanitizeFilterValues(mealTypes);
        java.util.List<String> selectedRegions = sanitizeFilterValues(regions);
        java.util.List<String> selectedByCategories = sanitizeFilterValues(byCategories);
        java.util.List<String> selectedTimeRanges = sanitizeFilterValues(timeRanges);
        java.util.List<String> selectedStyles = sanitizeFilterValues(styles);
        java.util.List<String> selectedNutritions = sanitizeFilterValues(nutritions);

        java.util.List<com.glodea.arhome.dto.RecipeDto> filteredRecipes = recipeService.searchRecipes(
            query,
            selectedMealTypes,
            selectedRegions,
            selectedByCategories,
            selectedTimeRanges,
            selectedStyles,
            selectedNutritions
        );
        int pageSize = RECIPES_MAX_ROWS * RECIPES_DEFAULT_COLUMNS;
        int totalRecipes = filteredRecipes.size();
        int totalPages = (int) Math.ceil(totalRecipes / (double) pageSize);
        if (totalPages == 0) {
            totalPages = 1;
        }
        int currentPage = page == null || page < 1 ? 1 : page;
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        int fromIndex = Math.min((currentPage - 1) * pageSize, totalRecipes);
        int toIndex = Math.min(fromIndex + pageSize, totalRecipes);
        filteredRecipes = filteredRecipes.subList(fromIndex, toIndex);

        model.addAttribute("recipes", filteredRecipes);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
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
    public String groceries(Model model) {
        User user = getCurrentUser();
        if (user != null) {
            model.addAttribute("initialManualItems", groceryService.listItems(user));
            model.addAttribute("initialRecipeBoxes", groceryService.getRecipeBoxes(user));
            model.addAttribute("initialPlanBoxes", groceryService.getPlanBoxes(user));
        } else {
            model.addAttribute("initialManualItems", java.util.List.of());
            model.addAttribute("initialRecipeBoxes", java.util.List.of());
            model.addAttribute("initialPlanBoxes", java.util.List.of());
        }
        return "groceries";
    }

    @GetMapping("/collection")
    public String collection(@RequestParam(value = "q", required = false) String query,
                             @RequestParam(value = "mealType", required = false) String mealType,
                             @RequestParam(value = "sort", required = false) String sort,
                             Model model) {
        User user = getCurrentUser();
        String normalizedQuery = query == null ? "" : query.trim();
        String normalizedMealType = mealType == null || mealType.isBlank() ? "All" : mealType.trim();
        String normalizedSort = sort == null || sort.isBlank() ? "recent" : sort.trim().toLowerCase();
        if (user != null) {
            model.addAttribute("favoriteRecipes", recipeService.searchFavoriteRecipesForUser(user, normalizedQuery, normalizedMealType, normalizedSort));
            model.addAttribute("favoriteRecipeIds", recipeService.getFavoriteRecipeIds(user));
        } else {
            model.addAttribute("favoriteRecipes", java.util.List.of());
        }
        model.addAttribute("collectionSearchQuery", normalizedQuery);
        model.addAttribute("collectionMealType", normalizedMealType);
        model.addAttribute("collectionSort", normalizedSort);
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
