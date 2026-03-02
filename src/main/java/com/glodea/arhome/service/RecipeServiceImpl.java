package com.glodea.arhome.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.glodea.arhome.dto.RecipeCreateRequest;
import com.glodea.arhome.dto.RecipeDto;
import com.glodea.arhome.entity.Recipe;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.RecipeRepository;
import com.glodea.arhome.repository.UserRepository;

@Service
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    public RecipeServiceImpl(RecipeRepository recipeRepository, UserRepository userRepository) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
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
        recipe.setImagePath(imagePath != null ? imagePath : trimToNull(request.getImagePath()));
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
            result.add(toDto(recipe));
        }
        return result;
    }

    @Override
    public List<RecipeDto> getAllRecipes() {
        List<Recipe> recipes = recipeRepository.findAllByOrderByCreatedAtDesc();
        List<RecipeDto> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            result.add(toDto(recipe));
        }
        return result;
    }

    @Override
    public List<RecipeDto> searchRecipes(String query) {
        List<Recipe> recipes;
        if (!StringUtils.hasText(query)) {
            recipes = recipeRepository.findAllByOrderByCreatedAtDesc();
        } else {
            String normalizedQuery = query.trim();
            List<String> terms = java.util.Arrays.stream(normalizedQuery.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .toList();

            if (terms.size() <= 1) {
                recipes = recipeRepository.findByTitleContainingIgnoreCaseOrIngredientsContainingIgnoreCaseOrderByCreatedAtDesc(
                    normalizedQuery,
                    normalizedQuery
                );
            } else {
                recipes = recipeRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(recipe -> {
                        String title = recipe.getTitle() == null ? "" : recipe.getTitle().toLowerCase();
                        String ingredients = recipe.getIngredients() == null ? "" : recipe.getIngredients().toLowerCase();
                        return terms.stream().allMatch(term -> title.contains(term) || ingredients.contains(term));
                    })
                    .toList();
            }
        }

        List<RecipeDto> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            result.add(toDto(recipe));
        }
        return result;
    }

    @Override
    public Recipe getRecipeById(Long recipeId) {
        return recipeRepository.findById(recipeId).orElseThrow();
    }

    @Override
    public Set<Long> getFavoriteRecipeIds(User user) {
        if (user == null || user.getFavoriteRecipes() == null) {
            return java.util.Collections.emptySet();
        }
        return user.getFavoriteRecipes().stream()
            .map(Recipe::getId)
            .filter(id -> id != null)
            .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public List<RecipeDto> getFavoriteRecipesForUser(User user) {
        if (user == null || user.getFavoriteRecipes() == null) {
            return List.of();
        }
        List<Recipe> favorites = new ArrayList<>(user.getFavoriteRecipes());
        favorites.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        List<RecipeDto> result = new ArrayList<>();
        for (Recipe recipe : favorites) {
            result.add(toDto(recipe));
        }
        return result;
    }

    @Override
    public boolean toggleFavorite(User user, Long recipeId) {
        if (user == null || recipeId == null) {
            return false;
        }
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow();
        if (recipe.getUser() != null && recipe.getUser().getId().equals(user.getId())) {
            return false;
        }
        if (user.getFavoriteRecipes() == null) {
            user.setFavoriteRecipes(new java.util.HashSet<>());
        }
        boolean removed = false;
        Recipe existing = null;
        for (Recipe fav : user.getFavoriteRecipes()) {
            if (fav.getId() != null && fav.getId().equals(recipeId)) {
                existing = fav;
                break;
            }
        }
        if (existing != null) {
            user.getFavoriteRecipes().remove(existing);
            removed = true;
        } else {
            user.getFavoriteRecipes().add(recipe);
        }
        userRepository.save(user);
        return !removed;
    }

    @Override
    public Recipe getRecipeForUser(User user, Long recipeId) {
        return recipeRepository.findByIdAndUser(recipeId, user)
            .orElseThrow();
    }

    @Override
    public Recipe updateRecipe(User user, Long recipeId, RecipeCreateRequest request, String imagePath) {
        Recipe recipe = recipeRepository.findByIdAndUser(recipeId, user)
            .orElseThrow();

        recipe.setTitle(request.getTitle().trim());
        recipe.setShortDescription(trimToNull(request.getShortDescription()));
        recipe.setPrepTime(request.getPrepTime().trim());
        recipe.setMealType(request.getMealType().trim());
        recipe.setIngredients(trimToNull(request.getIngredients()));
        recipe.setSteps(trimToNull(request.getSteps()));
        recipe.setInstructions(trimToNull(request.getInstructions()));
        if (imagePath != null) {
            recipe.setImagePath(imagePath);
        } else if (trimToNull(request.getImagePath()) != null) {
            recipe.setImagePath(trimToNull(request.getImagePath()));
        }
        recipe.setRegionTags(safeList(request.getRegionTags()));
        recipe.setStyleTags(safeList(request.getStyleTags()));
        recipe.setNutritionTags(safeList(request.getNutritionTags()));

        return recipeRepository.save(recipe);
    }

    @Override
    public void deleteRecipe(User user, Long recipeId) {
        Recipe recipe = recipeRepository.findByIdAndUser(recipeId, user)
            .orElseThrow();
        recipeRepository.delete(recipe);
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

    private RecipeDto toDto(Recipe recipe) {
        return new RecipeDto(
            recipe.getId(),
            recipe.getTitle(),
            recipe.getShortDescription(),
            recipe.getPrepTime(),
            recipe.getMealType(),
            recipe.getImagePath(),
            mergeTags(recipe),
            recipe.getUser() != null ? recipe.getUser().getId() : null
        );
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
