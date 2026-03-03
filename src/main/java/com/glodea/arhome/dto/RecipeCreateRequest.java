package com.glodea.arhome.dto;

import java.util.ArrayList;
import java.util.List;

public class RecipeCreateRequest {

    private String title;
    private String shortDescription;
    private String prepTime;
    private String mealType;
    private String ingredients;
    private List<RecipeIngredientInput> ingredientItems = new ArrayList<>();
    private String steps;
    private String instructions;
    private String imagePath;
    private List<String> regionTags = new ArrayList<>();
    private List<String> styleTags = new ArrayList<>();
    private List<String> nutritionTags = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getPrepTime() {
        return prepTime;
    }

    public void setPrepTime(String prepTime) {
        this.prepTime = prepTime;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public List<RecipeIngredientInput> getIngredientItems() {
        return ingredientItems;
    }

    public void setIngredientItems(List<RecipeIngredientInput> ingredientItems) {
        this.ingredientItems = ingredientItems;
    }

    public String getSteps() {
        return steps;
    }

    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public List<String> getRegionTags() {
        return regionTags;
    }

    public void setRegionTags(List<String> regionTags) {
        this.regionTags = regionTags;
    }

    public List<String> getStyleTags() {
        return styleTags;
    }

    public void setStyleTags(List<String> styleTags) {
        this.styleTags = styleTags;
    }

    public List<String> getNutritionTags() {
        return nutritionTags;
    }

    public void setNutritionTags(List<String> nutritionTags) {
        this.nutritionTags = nutritionTags;
    }
}
