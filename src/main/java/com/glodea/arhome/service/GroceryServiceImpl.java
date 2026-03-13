package com.glodea.arhome.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.glodea.arhome.dto.GroceryItemResponse;
import com.glodea.arhome.dto.GroceryRecipeBoxResponse;
import com.glodea.arhome.entity.GroceryItem;
import com.glodea.arhome.entity.Recipe;
import com.glodea.arhome.entity.RecipeIngredient;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.GroceryItemRepository;
import com.glodea.arhome.repository.RecipeRepository;

@Service
@Transactional
public class GroceryServiceImpl implements GroceryService {

    private static final Pattern LEGACY_INGREDIENT_PATTERN = Pattern
        .compile("^([0-9]+(?:[\\.,][0-9]+)?)\\s+([^\\s]+)\\s+(.+)$");

    private final GroceryItemRepository groceryItemRepository;
    private final RecipeRepository recipeRepository;

    public GroceryServiceImpl(GroceryItemRepository groceryItemRepository, RecipeRepository recipeRepository) {
        this.groceryItemRepository = groceryItemRepository;
        this.recipeRepository = recipeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroceryItemResponse> listItems(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return groceryItemRepository.findByUserIdAndSourceRecipeIdIsNullOrderByCreatedAtAsc(user.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroceryRecipeBoxResponse> getRecipeBoxes(User user) {
        if (user == null || user.getId() == null) {
            return List.of();
        }

        List<GroceryItem> recipeItems = groceryItemRepository
            .findByUserIdAndSourceRecipeIdIsNotNullOrderByCreatedAtAsc(user.getId());
        if (recipeItems.isEmpty()) {
            return List.of();
        }

        java.util.LinkedHashMap<Long, List<GroceryItemResponse>> groupedItems = new java.util.LinkedHashMap<>();
        java.util.LinkedHashMap<Long, String> groupedTitles = new java.util.LinkedHashMap<>();

        for (GroceryItem item : recipeItems) {
            Long sourceRecipeId = item.getSourceRecipeId();
            if (sourceRecipeId == null) {
                continue;
            }
            groupedItems.computeIfAbsent(sourceRecipeId, ignored -> new ArrayList<>()).add(toResponse(item));
            groupedTitles.putIfAbsent(sourceRecipeId, item.getSourceRecipeTitle() != null ? item.getSourceRecipeTitle() : "");
        }

        return groupedItems.entrySet().stream()
            .map(entry -> new GroceryRecipeBoxResponse(
                entry.getKey(),
                groupedTitles.getOrDefault(entry.getKey(), ""),
                entry.getValue()
            ))
            .toList();
    }

    @Override
    public List<GroceryRecipeBoxResponse> addRecipeItems(User user, Long recipeId) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Authentication required.");
        }
        if (recipeId == null) {
            throw new IllegalArgumentException("Recipe is required.");
        }

        Recipe recipe = recipeRepository.findById(recipeId)
            .orElseThrow(() -> new IllegalArgumentException("Recipe not found."));

        List<IngredientPayload> ingredients = extractIngredients(recipe);
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("Recipe has no ingredients.");
        }

        groceryItemRepository.deleteByUserIdAndSourceRecipeId(user.getId(), recipeId);

        for (IngredientPayload ingredient : ingredients) {
            GroceryItem item = new GroceryItem();
            item.setUser(user);
            item.setProductName(ingredient.productName());
            item.setQuantity(ingredient.quantity());
            item.setUnit(ingredient.unit());
            item.setBought(false);
            item.setSourceRecipeId(recipe.getId());
            item.setSourceRecipeTitle(recipe.getTitle());
            groceryItemRepository.save(item);
        }

        return getRecipeBoxes(user);
    }

    @Override
    public GroceryItemResponse addItem(User user, String productName, String quantity, String unit) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Authentication required.");
        }

        String normalizedName = productName == null ? "" : productName.trim();
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Product is required.");
        }

        String normalizedQuantity = quantity == null ? "" : quantity.trim();
        if (normalizedQuantity.isBlank()) {
            throw new IllegalArgumentException("Quantity is required.");
        }

        BigDecimal parsedQuantity;
        try {
            parsedQuantity = new BigDecimal(normalizedQuantity.replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid quantity.");
        }

        if (parsedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        }

        String normalizedUnit = unit == null ? "" : unit.trim();
        if (normalizedUnit.isBlank()) {
            normalizedUnit = "unit";
        }

        GroceryItem item = new GroceryItem();
        item.setUser(user);
        item.setProductName(normalizedName);
        item.setQuantity(parsedQuantity);
        item.setUnit(normalizedUnit);
        item.setBought(false);

        GroceryItem saved = groceryItemRepository.save(item);
        return toResponse(saved);
    }

    @Override
    public GroceryItemResponse setBought(User user, Long id, boolean bought) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Authentication required.");
        }

        GroceryItem item = groceryItemRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Item not found."));

        item.setBought(bought);
        GroceryItem saved = groceryItemRepository.save(item);
        return toResponse(saved);
    }

    @Override
    public void deleteItem(User user, Long id) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Authentication required.");
        }

        GroceryItem item = groceryItemRepository.findByIdAndUserId(id, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Item not found."));

        groceryItemRepository.delete(item);
    }

    private GroceryItemResponse toResponse(GroceryItem item) {
        return new GroceryItemResponse(
            item.getId(),
            item.getProductName(),
            item.getQuantity() != null ? item.getQuantity().stripTrailingZeros().toPlainString() : "",
            item.getUnit(),
            item.isBought(),
            item.getSourceRecipeId(),
            item.getSourceRecipeTitle()
        );
    }

    private List<IngredientPayload> extractIngredients(Recipe recipe) {
        if (recipe.getRecipeIngredients() != null && !recipe.getRecipeIngredients().isEmpty()) {
            return recipe.getRecipeIngredients().stream()
                .sorted(java.util.Comparator.comparing(RecipeIngredient::getPositionIndex,
                    java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .filter(item -> item.getIngredient() != null && item.getIngredient().getName() != null)
                .map(item -> {
                    String unit = item.getUnit() == null || item.getUnit().isBlank() ? "unit" : item.getUnit().trim();
                    BigDecimal quantity = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ONE;
                    return new IngredientPayload(item.getIngredient().getName().trim(), quantity, unit);
                })
                .filter(item -> !item.productName().isBlank() && item.quantity().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        }

        return parseLegacyIngredients(recipe.getIngredients());
    }

    private List<IngredientPayload> parseLegacyIngredients(String rawIngredients) {
        if (rawIngredients == null || rawIngredients.isBlank()) {
            return List.of();
        }

        List<IngredientPayload> items = new ArrayList<>();
        String[] lines = rawIngredients.split("\\r?\\n");
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            Matcher matcher = LEGACY_INGREDIENT_PATTERN.matcher(trimmed);
            if (matcher.matches()) {
                String quantityText = matcher.group(1).replace(',', '.');
                String unit = matcher.group(2).trim();
                String name = matcher.group(3).trim();
                try {
                    BigDecimal quantity = new BigDecimal(quantityText);
                    if (quantity.compareTo(BigDecimal.ZERO) > 0 && !name.isBlank()) {
                        items.add(new IngredientPayload(name, quantity, unit.isBlank() ? "unit" : unit));
                    }
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }

            items.add(new IngredientPayload(trimmed, BigDecimal.ONE, "unit"));
        }

        return items;
    }

    private record IngredientPayload(String productName, BigDecimal quantity, String unit) {
    }
}
