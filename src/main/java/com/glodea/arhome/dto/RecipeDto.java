package com.glodea.arhome.dto;

import java.util.ArrayList;
import java.util.List;

public class RecipeDto {

    private Long id;
    private String title;
    private String shortDescription;
    private String prepTime;
    private String mealType;
    private String imagePath;
    private List<String> tags = new ArrayList<>();
    private Long ownerId;

    public RecipeDto() {
    }

    public RecipeDto(Long id, String title, String shortDescription, String prepTime, String mealType, String imagePath, List<String> tags, Long ownerId) {
        this.id = id;
        this.title = title;
        this.shortDescription = shortDescription;
        this.prepTime = prepTime;
        this.mealType = mealType;
        this.imagePath = imagePath;
        this.tags = tags;
        this.ownerId = ownerId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
}
