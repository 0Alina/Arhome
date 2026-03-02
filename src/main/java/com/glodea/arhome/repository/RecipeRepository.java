package com.glodea.arhome.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.glodea.arhome.entity.Recipe;
import com.glodea.arhome.entity.User;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByUserOrderByCreatedAtDesc(User user);
    java.util.Optional<Recipe> findByIdAndUser(Long id, User user);
    List<Recipe> findAllByOrderByCreatedAtDesc();
    List<Recipe> findByTitleContainingIgnoreCaseOrIngredientsContainingIgnoreCaseOrderByCreatedAtDesc(String title, String ingredients);
}
