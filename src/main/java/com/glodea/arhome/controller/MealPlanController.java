package com.glodea.arhome.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.glodea.arhome.dto.MealPlanGenerateRequest;
import com.glodea.arhome.entity.MealPlan;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.UserRepository;
import com.glodea.arhome.service.AiMealPlanService;
import com.glodea.arhome.service.GroceryService;
import com.glodea.arhome.service.MealPlanService;

@RestController
@RequestMapping("/api/meal-plans")
public class MealPlanController {

    private final AiMealPlanService aiMealPlanService;
    private final MealPlanService mealPlanService;
    private final UserRepository userRepository;
    private final GroceryService groceryService;

    public MealPlanController(AiMealPlanService aiMealPlanService,
                              MealPlanService mealPlanService,
                              UserRepository userRepository,
                              GroceryService groceryService) {
        this.aiMealPlanService = aiMealPlanService;
        this.mealPlanService = mealPlanService;
        this.userRepository = userRepository;
        this.groceryService = groceryService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody MealPlanGenerateRequest request) {
        try {
            JsonNode plan = aiMealPlanService.generatePlan(request);
            User user = getCurrentUser();
            MealPlan saved = mealPlanService.savePlan(user, request.getSource(), plan);
            if (user != null) {
                groceryService.addPlanItems(user, saved.getId(), saved.getTitle(), plan);
            }
            return ResponseEntity.ok(Map.of(
                "ok", true,
                "plan", plan,
                "planId", saved.getId(),
                "createdAt", saved.getCreatedAt(),
                "source", saved.getSource(),
                "title", saved.getTitle(),
                "active", saved.isActive()
            ));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(503).body(Map.of("ok", false, "message", ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Authentication required."));
        }

        List<MealPlan> plans = mealPlanService.listPlans(user);
        List<Map<String, Object>> payload = new ArrayList<>();
        for (MealPlan plan : plans) {
            JsonNode planJson = mealPlanService.readPlan(plan);
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("planId", plan.getId());
            row.put("createdAt", plan.getCreatedAt());
            row.put("completedAt", plan.getCompletedAt());
            row.put("source", plan.getSource());
            row.put("title", plan.getTitle());
            row.put("active", plan.isActive());
            row.put("plan", planJson);
            payload.add(row);
        }
        return ResponseEntity.ok(Map.of("ok", true, "plans", payload));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable("id") Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Authentication required."));
        }

        try {
            MealPlan updated = mealPlanService.markCompleted(user, id);
            groceryService.removePlanItems(user, updated.getId());
            return ResponseEntity.ok(Map.of(
                "ok", true,
                "planId", updated.getId(),
                "completedAt", updated.getCompletedAt()
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", ex.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Authentication required."));
        }

        try {
            mealPlanService.deletePlan(user, id);
            groceryService.removePlanItems(user, id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", ex.getMessage()));
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<?> latest() {
        User user = getCurrentUser();
        Optional<MealPlan> latest = mealPlanService.getLatestPlan(user);
        if (latest.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        MealPlan plan = latest.get();
        JsonNode payload = mealPlanService.readPlan(plan);
        return ResponseEntity.ok(Map.of(
            "ok", true,
            "plan", payload,
            "planId", plan.getId(),
            "createdAt", plan.getCreatedAt(),
            "source", plan.getSource(),
            "title", plan.getTitle()
        ));
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
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            return userDetails.getUsername();
        }
        if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauthUser) {
            Object emailAttr = oauthUser.getAttributes().get("email");
            return emailAttr != null ? emailAttr.toString() : null;
        }
        if (principal != null) {
            return principal.toString();
        }
        return null;
    }
}
