package com.glodea.arhome.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.glodea.arhome.dto.MealPlanGenerateRequest;
import com.glodea.arhome.entity.MealPlan;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.UserRepository;
import com.glodea.arhome.service.AiMealPlanService;
import com.glodea.arhome.service.MealPlanService;

@RestController
@RequestMapping("/api/meal-plans")
public class MealPlanController {

    private final AiMealPlanService aiMealPlanService;
    private final MealPlanService mealPlanService;
    private final UserRepository userRepository;

    public MealPlanController(AiMealPlanService aiMealPlanService,
                              MealPlanService mealPlanService,
                              UserRepository userRepository) {
        this.aiMealPlanService = aiMealPlanService;
        this.mealPlanService = mealPlanService;
        this.userRepository = userRepository;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody MealPlanGenerateRequest request) {
        try {
            JsonNode plan = aiMealPlanService.generatePlan(request);
            User user = getCurrentUser();
            MealPlan saved = mealPlanService.savePlan(user, request.getSource(), plan);
            return ResponseEntity.ok(Map.of(
                "ok", true,
                "plan", plan,
                "planId", saved.getId(),
                "createdAt", saved.getCreatedAt(),
                "source", saved.getSource()
            ));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(503).body(Map.of("ok", false, "message", ex.getMessage()));
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
