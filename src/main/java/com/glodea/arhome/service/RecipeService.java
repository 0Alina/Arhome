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
    List<RecipeDto> searchRecipes(String query);
    List<RecipeDto> searchRecipes(String query,
                                  List<String> mealTypes,
                                  List<String> regions,
                                  List<String> byCategories,
                                  List<String> timeRanges,
                                  List<String> styles,
                                  List<String> nutritions);
    Recipe getRecipeById(Long recipeId);
    Set<Long> getFavoriteRecipeIds(User user);
    List<RecipeDto> getFavoriteRecipesForUser(User user);
    List<RecipeDto> searchFavoriteRecipesForUser(User user, String query);
    List<RecipeDto> searchFavoriteRecipesForUser(User user, String query, String mealType, String sort);
    boolean toggleFavorite(User user, Long recipeId);
}
