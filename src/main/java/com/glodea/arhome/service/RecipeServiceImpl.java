package com.glodea.arhome.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.glodea.arhome.dto.RecipeCreateRequest;
import com.glodea.arhome.dto.RecipeDto;
import com.glodea.arhome.dto.RecipeIngredientInput;
import com.glodea.arhome.entity.Ingredient;
import com.glodea.arhome.entity.Recipe;
import com.glodea.arhome.entity.RecipeIngredient;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.IngredientRepository;
import com.glodea.arhome.repository.RecipeRepository;
import com.glodea.arhome.repository.UserRepository;

@Service
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final IngredientRepository ingredientRepository;

    public RecipeServiceImpl(RecipeRepository recipeRepository, UserRepository userRepository, IngredientRepository ingredientRepository) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.ingredientRepository = ingredientRepository;
    }

    @Override
    public List<String> suggestIngredientNames(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        String normalized = query.trim();
        if (normalized.length() < 2) {
            return List.of();
        }

        return ingredientRepository.findTop12ByNameContainingIgnoreCaseOrderByNameAsc(normalized).stream()
            .map(Ingredient::getName)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();
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
        recipe.setSteps(trimToNull(request.getSteps()));
        recipe.setInstructions(trimToNull(request.getInstructions()));
        recipe.setImagePath(imagePath != null ? imagePath : trimToNull(request.getImagePath()));
        recipe.setRegionTags(safeList(request.getRegionTags()));
        recipe.setStyleTags(safeList(request.getStyleTags()));
        recipe.setNutritionTags(safeList(request.getNutritionTags()));
        applyStructuredIngredients(recipe, request);

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
        return searchRecipes(query, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Override
    public List<RecipeDto> searchRecipes(String query,
                                         List<String> mealTypes,
                                         List<String> regions,
                                         List<String> byCategories,
                                         List<String> timeRanges,
                                         List<String> styles,
                                         List<String> nutritions) {
        List<String> searchTerms = parseSearchTerms(query);
        List<String> normalizedMealTypes = normalizeFilters(mealTypes);
        List<String> normalizedRegions = normalizeFilters(regions);
        List<String> normalizedByCategories = normalizeFilters(byCategories);
        List<String> normalizedTimeRanges = normalizeFilters(timeRanges);
        List<String> normalizedStyles = normalizeFilters(styles);
        List<String> normalizedNutritions = normalizeFilters(nutritions);

        List<Recipe> recipes = recipeRepository.findAllByOrderByCreatedAtDesc().stream()
            .filter(recipe -> matchesSearchTerms(recipe, searchTerms))
            .filter(recipe -> matchesSingleValue(recipe.getMealType(), normalizedMealTypes))
            .filter(recipe -> matchesAnyTag(recipe.getRegionTags(), normalizedRegions))
            .filter(recipe -> matchesByCategory(recipe, normalizedByCategories))
            .filter(recipe -> matchesTimeRanges(recipe, normalizedTimeRanges))
            .filter(recipe -> matchesAnyTag(recipe.getStyleTags(), normalizedStyles))
            .filter(recipe -> matchesNutrition(recipe, normalizedNutritions))
            .toList();

        List<RecipeDto> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            result.add(toDto(recipe));
        }
        return result;
    }

    private List<String> parseSearchTerms(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        return java.util.Arrays.stream(query.trim().split(","))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(String::toLowerCase)
            .distinct()
            .toList();
    }

    private List<String> normalizeFilters(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(String::toLowerCase)
            .distinct()
            .toList();
    }

    private boolean matchesSearchTerms(Recipe recipe, List<String> terms) {
        if (terms.isEmpty()) {
            return true;
        }
        String title = recipe.getTitle() == null ? "" : recipe.getTitle().toLowerCase();
        String ingredients = recipe.getIngredients() == null ? "" : recipe.getIngredients().toLowerCase();
        List<String> structuredIngredientNames = recipe.getRecipeIngredients() == null
            ? List.of()
            : recipe.getRecipeIngredients().stream()
                .map(RecipeIngredient::getIngredient)
                .filter(java.util.Objects::nonNull)
                .map(Ingredient::getName)
                .filter(StringUtils::hasText)
                .map(String::toLowerCase)
                .toList();
        return terms.stream().allMatch(term ->
            title.contains(term)
                || ingredients.contains(term)
                || structuredIngredientNames.stream().anyMatch(name -> name.contains(term))
        );
    }

    private boolean matchesSingleValue(String value, List<String> filters) {
        if (filters.isEmpty()) {
            return true;
        }
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return filters.contains(normalized);
    }

    private boolean matchesAnyTag(List<String> tags, List<String> filters) {
        if (filters.isEmpty()) {
            return true;
        }
        if (tags == null || tags.isEmpty()) {
            return false;
        }
        java.util.Set<String> normalizedTags = tags.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(String::toLowerCase)
            .collect(java.util.stream.Collectors.toSet());
        return filters.stream().anyMatch(normalizedTags::contains);
    }

    private boolean matchesByCategory(Recipe recipe, List<String> byFilters) {
        if (byFilters.isEmpty()) {
            return true;
        }
        String category = recipe.getUser() != null ? recipe.getUser().getCategory() : null;
        if (!StringUtils.hasText(category)) {
            return false;
        }
        String normalizedCategory = normalizeGenericText(category);
        return byFilters.stream()
            .map(this::normalizeGenericText)
            .anyMatch(normalizedCategory::contains);
    }

    private boolean matchesTimeRanges(Recipe recipe, List<String> selectedRanges) {
        if (selectedRanges.isEmpty()) {
            return true;
        }
        Integer minutes = parsePrepTimeToMinutes(recipe.getPrepTime());
        if (minutes == null) {
            return false;
        }
        return selectedRanges.stream().anyMatch(range -> isMinutesInRange(minutes, range));
    }

    private Integer parsePrepTimeToMinutes(String prepTime) {
        if (!StringUtils.hasText(prepTime)) {
            return null;
        }
        String value = prepTime.trim().toLowerCase();
        java.util.regex.Matcher hourMatcher = java.util.regex.Pattern.compile("(\\d+)\\s*h").matcher(value);
        java.util.regex.Matcher minMatcher = java.util.regex.Pattern.compile("(\\d+)\\s*(min|m)").matcher(value);

        int total = 0;
        boolean matched = false;

        if (hourMatcher.find()) {
            total += Integer.parseInt(hourMatcher.group(1)) * 60;
            matched = true;
        }
        if (minMatcher.find()) {
            total += Integer.parseInt(minMatcher.group(1));
            matched = true;
        }
        if (!matched) {
            java.util.regex.Matcher numberMatcher = java.util.regex.Pattern.compile("(\\d+)").matcher(value);
            if (numberMatcher.find()) {
                total += Integer.parseInt(numberMatcher.group(1));
                matched = true;
            }
        }

        return matched ? total : null;
    }

    private boolean isMinutesInRange(int minutes, String range) {
        return switch (range) {
            case "< 15 min" -> minutes < 15;
            case "15 min · 30 min" -> minutes >= 15 && minutes <= 30;
            case "30 min · 1 h" -> minutes >= 30 && minutes <= 60;
            case "1 h · 2 h" -> minutes >= 60 && minutes <= 120;
            case "> 2 h" -> minutes > 120;
            default -> false;
        };
    }

    private boolean matchesNutrition(Recipe recipe, List<String> nutritionFilters) {
        if (nutritionFilters.isEmpty()) {
            return true;
        }
        if (recipe.getNutritionTags() == null || recipe.getNutritionTags().isEmpty()) {
            return false;
        }
        java.util.Set<String> normalizedTags = recipe.getNutritionTags().stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(String::toLowerCase)
            .map(this::normalizeNutritionValue)
            .collect(java.util.stream.Collectors.toSet());

        return nutritionFilters.stream()
            .map(this::normalizeNutritionValue)
            .anyMatch(normalizedTags::contains);
    }

    private String normalizeNutritionValue(String value) {
        String normalized = value.replace("fibre", "fiber");
        return normalized;
    }

    private String normalizeGenericText(String value) {
        return value == null
            ? ""
            : value.trim().toLowerCase().replace('’', '\'');
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
    public List<RecipeDto> searchFavoriteRecipesForUser(User user, String query) {
        return searchFavoriteRecipesForUser(user, query, null, "recent");
    }

    @Override
    public List<RecipeDto> searchFavoriteRecipesForUser(User user, String query, String mealType, String sort) {
        if (user == null || user.getFavoriteRecipes() == null) {
            return List.of();
        }

        List<String> searchTerms = parseSearchTerms(query);
        boolean hasMealTypeFilter = StringUtils.hasText(mealType) && !"all".equalsIgnoreCase(mealType.trim());
        String normalizedMealType = hasMealTypeFilter ? mealType.trim().toLowerCase() : "";
        String normalizedSort = StringUtils.hasText(sort) ? sort.trim().toLowerCase() : "recent";

        List<Recipe> favorites = new ArrayList<>(user.getFavoriteRecipes()).stream()
            .filter(recipe -> matchesSearchTerms(recipe, searchTerms))
            .toList();

        if (hasMealTypeFilter) {
            favorites = favorites.stream()
                .filter(recipe -> recipe.getMealType() != null && recipe.getMealType().trim().toLowerCase().equals(normalizedMealType))
                .toList();
        }

        List<Recipe> sortedFavorites = new ArrayList<>(favorites);
        switch (normalizedSort) {
            case "time" -> sortedFavorites.sort(java.util.Comparator
                .comparing((Recipe recipe) -> {
                    Integer minutes = parsePrepTimeToMinutes(recipe.getPrepTime());
                    return minutes != null ? minutes : Integer.MAX_VALUE;
                })
                .thenComparing(Recipe::getCreatedAt, java.util.Comparator.reverseOrder()));
            case "alpha" -> sortedFavorites.sort(java.util.Comparator
                .comparing((Recipe recipe) -> recipe.getTitle() == null ? "" : recipe.getTitle().toLowerCase()));
            default -> sortedFavorites.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }

        List<RecipeDto> result = new ArrayList<>();
        for (Recipe recipe : sortedFavorites) {
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
        applyStructuredIngredients(recipe, request);

        return recipeRepository.save(recipe);
    }

    private void applyStructuredIngredients(Recipe recipe, RecipeCreateRequest request) {
        List<RecipeIngredientInput> inputs = request.getIngredientItems() == null ? List.of() : request.getIngredientItems();
        List<RecipeIngredient> structuredIngredients = new ArrayList<>();

        int index = 0;
        for (RecipeIngredientInput input : inputs) {
            if (input == null || !StringUtils.hasText(input.getName())) {
                continue;
            }
            if (!StringUtils.hasText(input.getQuantity()) || !StringUtils.hasText(input.getUnit())) {
                continue;
            }

            java.math.BigDecimal quantity = parseQuantity(input.getQuantity());
            if (quantity == null || quantity.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                continue;
            }

            String ingredientName = normalizeIngredientName(input.getName());
            if (!StringUtils.hasText(ingredientName)) {
                continue;
            }

            Ingredient ingredient = ingredientRepository.findByNameIgnoreCase(ingredientName)
                .orElseGet(() -> {
                    Ingredient created = new Ingredient();
                    created.setName(ingredientName);
                    return ingredientRepository.save(created);
                });

            RecipeIngredient recipeIngredient = new RecipeIngredient();
            recipeIngredient.setRecipe(recipe);
            recipeIngredient.setIngredient(ingredient);
            recipeIngredient.setQuantity(quantity);
            recipeIngredient.setUnit(input.getUnit().trim());
            recipeIngredient.setPositionIndex(index++);
            structuredIngredients.add(recipeIngredient);
        }

        if (recipe.getRecipeIngredients() == null) {
            recipe.setRecipeIngredients(new ArrayList<>());
        }
        recipe.getRecipeIngredients().clear();
        recipe.getRecipeIngredients().addAll(structuredIngredients);
        recipe.setIngredients(buildLegacyIngredientsText(structuredIngredients));
    }

    private java.math.BigDecimal parseQuantity(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        String normalized = rawValue.trim().replace(',', '.');
        try {
            return new java.math.BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeIngredientName(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            return null;
        }
        String trimmed = rawName.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return null;
        }
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    private String buildLegacyIngredientsText(List<RecipeIngredient> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return items.stream()
            .map(item -> item.getQuantity().stripTrailingZeros().toPlainString()
                + " "
                + item.getUnit()
                + " "
                + item.getIngredient().getName())
            .collect(java.util.stream.Collectors.joining("\n"));
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
