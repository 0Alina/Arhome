package com.glodea.arhome.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.glodea.arhome.entity.MealPlan;
import com.glodea.arhome.entity.User;

public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    Optional<MealPlan> findTopByUserOrderByCreatedAtDesc(User user);
    Optional<MealPlan> findTopByUserIsNullOrderByCreatedAtDesc();
    List<MealPlan> findByUserOrderByCreatedAtDesc(User user);
    Optional<MealPlan> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
