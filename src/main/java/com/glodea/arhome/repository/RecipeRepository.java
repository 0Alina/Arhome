package com.glodea.arhome.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.glodea.arhome.entity.Recipe;
import com.glodea.arhome.entity.User;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByUserOrderByCreatedAtDesc(User user);
}
