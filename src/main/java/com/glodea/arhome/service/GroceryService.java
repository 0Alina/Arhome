package com.glodea.arhome.service;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.glodea.arhome.dto.GroceryItemResponse;
import com.glodea.arhome.dto.GroceryPlanBoxResponse;
import com.glodea.arhome.dto.GroceryRecipeBoxResponse;
import com.glodea.arhome.entity.User;

public interface GroceryService {

    List<GroceryItemResponse> listItems(User user);

    List<GroceryRecipeBoxResponse> getRecipeBoxes(User user);

    List<GroceryPlanBoxResponse> getPlanBoxes(User user);

    List<GroceryRecipeBoxResponse> addRecipeItems(User user, Long recipeId);

    List<GroceryPlanBoxResponse> addPlanItems(User user, Long planId, String planTitle, JsonNode plan);

    void removeRecipeItems(User user, Long recipeId);

    void removePlanItems(User user, Long planId);

    GroceryItemResponse addItem(User user, String productName, String quantity, String unit);

    GroceryItemResponse setBought(User user, Long id, boolean bought);

    void deleteItem(User user, Long id);
}
