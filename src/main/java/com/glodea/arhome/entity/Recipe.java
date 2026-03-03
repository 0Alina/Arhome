package com.glodea.arhome.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1200)
    private String shortDescription;

    @Column(nullable = false)
    private String prepTime;

    @Column(nullable = false)
    private String mealType;

    @Column(length = 5000)
    private String ingredients;

    @OneToMany(mappedBy = "recipe", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<RecipeIngredient> recipeIngredients = new ArrayList<>();

    @Column(length = 8000)
    private String steps;

    @Column(length = 8000)
    private String instructions;

    private String imagePath;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recipe_region_tags", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "tag")
    private List<String> regionTags = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recipe_style_tags", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "tag")
    private List<String> styleTags = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recipe_nutrition_tags", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "tag")
    private List<String> nutritionTags = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

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

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public List<RecipeIngredient> getRecipeIngredients() {
        return recipeIngredients;
    }

    public void setRecipeIngredients(List<RecipeIngredient> recipeIngredients) {
        this.recipeIngredients = recipeIngredients;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
