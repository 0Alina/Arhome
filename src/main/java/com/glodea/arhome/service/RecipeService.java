package com.glodea.arhome.service;

import com.glodea.arhome.dto.RecipeCreateRequest;
import com.glodea.arhome.dto.RecipeDto;
import com.glodea.arhome.entity.Recipe;
import com.glodea.arhome.entity.User;

import java.util.List;

public interface RecipeService {
    Recipe createRecipe(User user, RecipeCreateRequest request, String imagePath);
    List<RecipeDto> getRecipesForUser(User user);
}
