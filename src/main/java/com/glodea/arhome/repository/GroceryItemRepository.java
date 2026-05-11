package com.glodea.arhome.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.glodea.arhome.entity.GroceryItem;

public interface GroceryItemRepository extends JpaRepository<GroceryItem, Long> {

    List<GroceryItem> findByUserIdAndSourceRecipeIdIsNullAndSourcePlanIdIsNullOrderByCreatedAtAsc(Long userId);

    List<GroceryItem> findByUserIdAndSourceRecipeIdIsNotNullOrderBySourceRecipeIdAscCreatedAtAsc(Long userId);

    List<GroceryItem> findByUserIdAndSourcePlanIdIsNotNullOrderBySourcePlanIdAscCreatedAtAsc(Long userId);

    void deleteByUserIdAndSourceRecipeId(Long userId, Long sourceRecipeId);

    void deleteByUserIdAndSourcePlanId(Long userId, Long sourcePlanId);

    Optional<GroceryItem> findByIdAndUserId(Long id, Long userId);
}
