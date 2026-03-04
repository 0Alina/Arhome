package com.glodea.arhome.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.glodea.arhome.entity.RecipeRating;

public interface RecipeRatingRepository extends JpaRepository<RecipeRating, Long> {

    Optional<RecipeRating> findByRecipeIdAndUserId(Long recipeId, Long userId);

    @Query("select r from RecipeRating r join fetch r.user where r.recipe.id = :recipeId")
    List<RecipeRating> findByRecipeId(@Param("recipeId") Long recipeId);

    long countByRecipeId(Long recipeId);

    @Query("select avg(r.ratingValue) from RecipeRating r where r.recipe.id = :recipeId")
    Double findAverageByRecipeId(@Param("recipeId") Long recipeId);

    @Query("select avg(r.ratingValue) from RecipeRating r where r.recipe.user.id = :userId")
    Double findAverageByRecipeOwnerId(@Param("userId") Long userId);
}
