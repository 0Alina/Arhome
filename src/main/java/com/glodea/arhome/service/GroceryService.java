package com.glodea.arhome.service;

import java.util.List;

import com.glodea.arhome.dto.GroceryItemResponse;
import com.glodea.arhome.dto.GroceryRecipeBoxResponse;
import com.glodea.arhome.entity.User;

public interface GroceryService {

    List<GroceryItemResponse> listItems(User user);

    List<GroceryRecipeBoxResponse> getRecipeBoxes(User user);

    List<GroceryRecipeBoxResponse> addRecipeItems(User user, Long recipeId);

    void removeRecipeItems(User user, Long recipeId);

    GroceryItemResponse addItem(User user, String productName, String quantity, String unit);

    GroceryItemResponse setBought(User user, Long id, boolean bought);

    void deleteItem(User user, Long id);
}
