package com.glodea.arhome.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            mealPlan.setActive(true);
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

    public List<MealPlan> listPlans(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return mealPlanRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public MealPlan markCompleted(User user, Long planId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Authentication required.");
        }
        if (planId == null) {
            throw new IllegalArgumentException("Plan is required.");
        }

        MealPlan plan = mealPlanRepository.findByIdAndUserId(planId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found."));
        if (!plan.isActive()) {
            return plan;
        }
        plan.setActive(false);
        plan.setCompletedAt(Instant.now());
        return mealPlanRepository.save(plan);
    }

    @Transactional
    public void deletePlan(User user, Long planId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Authentication required.");
        }
        if (planId == null) {
            throw new IllegalArgumentException("Plan is required.");
        }

        MealPlan existing = mealPlanRepository.findByIdAndUserId(planId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Plan not found."));
        mealPlanRepository.deleteByIdAndUserId(existing.getId(), user.getId());
    }

    public JsonNode readPlan(MealPlan mealPlan) {
        try {
            return objectMapper.readTree(mealPlan.getPlanJson());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse stored meal plan: " + ex.getMessage(), ex);
        }
    }
}
