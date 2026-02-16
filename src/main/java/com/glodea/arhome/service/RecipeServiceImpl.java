package com.glodea.arhome.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.glodea.arhome.dto.RecipeCreateRequest;
import com.glodea.arhome.dto.RecipeDto;
import com.glodea.arhome.entity.Recipe;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.RecipeRepository;

@Service
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeServiceImpl(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    public Recipe createRecipe(User user, RecipeCreateRequest request, String imagePath) {
        if (!StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getPrepTime()) || !StringUtils.hasText(request.getMealType())) {
            throw new IllegalArgumentException("Missing required fields");
        }

        Recipe recipe = new Recipe();
        recipe.setUser(user);
        recipe.setTitle(request.getTitle().trim());
        recipe.setShortDescription(trimToNull(request.getShortDescription()));
        recipe.setPrepTime(request.getPrepTime().trim());
        recipe.setMealType(request.getMealType().trim());
        recipe.setIngredients(trimToNull(request.getIngredients()));
        recipe.setSteps(trimToNull(request.getSteps()));
        recipe.setInstructions(trimToNull(request.getInstructions()));
        recipe.setImagePath(imagePath);
        recipe.setRegionTags(safeList(request.getRegionTags()));
        recipe.setStyleTags(safeList(request.getStyleTags()));
        recipe.setNutritionTags(safeList(request.getNutritionTags()));

        return recipeRepository.save(recipe);
    }

    @Override
    public List<RecipeDto> getRecipesForUser(User user) {
        List<Recipe> recipes = recipeRepository.findByUserOrderByCreatedAtDesc(user);
        List<RecipeDto> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            result.add(new RecipeDto(
                recipe.getId(),
                recipe.getTitle(),
                recipe.getShortDescription(),
                recipe.getPrepTime(),
                recipe.getMealType(),
                mergeTags(recipe)
            ));
        }
        return result;
    }

    private List<String> mergeTags(Recipe recipe) {
        List<String> tags = new ArrayList<>();
        if (recipe.getRegionTags() != null) {
            tags.addAll(recipe.getRegionTags());
        }
        if (recipe.getStyleTags() != null) {
            tags.addAll(recipe.getStyleTags());
        }
        if (recipe.getNutritionTags() != null) {
            tags.addAll(recipe.getNutritionTags());
        }
        return tags;
    }

    private List<String> safeList(List<String> input) {
        return input == null ? new ArrayList<>() : new ArrayList<>(input);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
