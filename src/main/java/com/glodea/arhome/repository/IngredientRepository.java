package com.glodea.arhome.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.glodea.arhome.entity.Ingredient;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByNameIgnoreCase(String name);
    java.util.List<Ingredient> findTop12ByNameContainingIgnoreCaseOrderByNameAsc(String name);
}
