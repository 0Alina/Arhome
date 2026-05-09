package com.glodea.arhome.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.glodea.arhome.entity.MealPlan;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.MealPlanRepository;

@Service
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final ObjectMapper objectMapper;

    public MealPlanService(MealPlanRepository mealPlanRepository, ObjectMapper objectMapper) {
        this.mealPlanRepository = mealPlanRepository;
        this.objectMapper = objectMapper;
    }

    public MealPlan savePlan(User user, String source, JsonNode plan) {
        try {
            MealPlan mealPlan = new MealPlan();
            mealPlan.setUser(user);
            mealPlan.setSource(source != null ? source : "unknown");
            mealPlan.setTitle(plan != null && plan.hasNonNull("title") ? plan.get("title").asText() : null);
            mealPlan.setPlanJson(objectMapper.writeValueAsString(plan));
            return mealPlanRepository.save(mealPlan);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to save meal plan: " + ex.getMessage(), ex);
        }
    }

    public Optional<MealPlan> getLatestPlan(User user) {
        if (user != null) {
            return mealPlanRepository.findTopByUserOrderByCreatedAtDesc(user);
        }
        return mealPlanRepository.findTopByUserIsNullOrderByCreatedAtDesc();
    }

    public JsonNode readPlan(MealPlan mealPlan) {
        try {
            return objectMapper.readTree(mealPlan.getPlanJson());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse stored meal plan: " + ex.getMessage(), ex);
        }
    }
}
