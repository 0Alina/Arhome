package com.glodea.arhome.service;

import com.glodea.arhome.dto.RecipeCreateRequest;
import com.glodea.arhome.dto.RecipeDto;
import com.glodea.arhome.entity.Recipe;
import com.glodea.arhome.entity.User;

import java.util.List;
import java.util.Set;

public interface RecipeService {
    Recipe createRecipe(User user, RecipeCreateRequest request, String imagePath);
    List<RecipeDto> getRecipesForUser(User user);
    Recipe getRecipeForUser(User user, Long recipeId);
    Recipe updateRecipe(User user, Long recipeId, RecipeCreateRequest request, String imagePath);
    void deleteRecipe(User user, Long recipeId);
    List<RecipeDto> getAllRecipes();
    Recipe getRecipeById(Long recipeId);
    Set<Long> getFavoriteRecipeIds(User user);
    List<RecipeDto> getFavoriteRecipesForUser(User user);
    boolean toggleFavorite(User user, Long recipeId);
}
