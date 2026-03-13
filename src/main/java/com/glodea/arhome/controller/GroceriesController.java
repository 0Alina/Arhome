package com.glodea.arhome.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.glodea.arhome.dto.GroceryItemBoughtRequest;
import com.glodea.arhome.dto.GroceryItemCreateRequest;
import com.glodea.arhome.dto.GroceryItemResponse;
import com.glodea.arhome.dto.GroceryRecipeBoxResponse;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.UserRepository;
import com.glodea.arhome.service.GroceryService;

@RestController
@RequestMapping("/groceries/items")
public class GroceriesController {

    private final GroceryService groceryService;
    private final UserRepository userRepository;

    public GroceriesController(GroceryService groceryService, UserRepository userRepository) {
        this.groceryService = groceryService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> listItems() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Authentication required."));
        }
        List<GroceryItemResponse> items = groceryService.listItems(user);
        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<?> addItem(@RequestBody GroceryItemCreateRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Authentication required."));
        }

        try {
            GroceryItemResponse item = groceryService.addItem(user, request.getProductName(), request.getQuantity(), request.getUnit());
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", ex.getMessage()));
        }
    }

    @GetMapping("/from-recipe")
    public ResponseEntity<?> getRecipeBox() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Authentication required."));
        }
        List<GroceryRecipeBoxResponse> boxes = groceryService.getRecipeBoxes(user);
        return ResponseEntity.ok(boxes);
    }

    @PostMapping("/from-recipe/{recipeId}")
    public ResponseEntity<?> addRecipeToGroceries(@PathVariable("recipeId") Long recipeId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Authentication required."));
        }

        try {
            List<GroceryRecipeBoxResponse> boxes = groceryService.addRecipeItems(user, recipeId);
            return ResponseEntity.ok(boxes);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", ex.getMessage()));
        }
    }

    @DeleteMapping("/from-recipe/{recipeId}")
    public ResponseEntity<?> removeRecipeFromGroceries(@PathVariable("recipeId") Long recipeId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Authentication required."));
        }

        try {
            groceryService.removeRecipeItems(user, recipeId);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/bought")
    public ResponseEntity<?> setBought(@PathVariable("id") Long id, @RequestBody GroceryItemBoughtRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Authentication required."));
        }

        if (request.getBought() == null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Missing bought value."));
        }

        try {
            GroceryItemResponse item = groceryService.setBought(user, id, request.getBought());
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(@PathVariable("id") Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Authentication required."));
        }

        try {
            groceryService.deleteItem(user, id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", ex.getMessage()));
        }
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
}
