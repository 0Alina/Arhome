package com.glodea.arhome.dto;

import java.util.List;

public class GroceryRecipeBoxResponse {

    private Long recipeId;
    private String recipeTitle;
    private List<GroceryItemResponse> items;

    public GroceryRecipeBoxResponse() {
    }

    public GroceryRecipeBoxResponse(Long recipeId, String recipeTitle, List<GroceryItemResponse> items) {
        this.recipeId = recipeId;
        this.recipeTitle = recipeTitle;
        this.items = items;
    }

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeTitle() {
        return recipeTitle;
    }

    public void setRecipeTitle(String recipeTitle) {
        this.recipeTitle = recipeTitle;
    }

    public List<GroceryItemResponse> getItems() {
        return items;
    }

    public void setItems(List<GroceryItemResponse> items) {
        this.items = items;
    }
}
