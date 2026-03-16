package com.glodea.arhome.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

@Entity
@Table(
    name = "grocery_items",
    indexes = {
        @Index(name = "idx_grocery_items_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_grocery_items_user_recipe_created", columnList = "user_id, source_recipe_id, created_at")
    }
)
public class GroceryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 160)
    private String productName;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 24)
    private String unit;

    @Column(nullable = false)
    private boolean bought;

    @Column(name = "source_recipe_id")
    private Long sourceRecipeId;

    @Column(name = "source_recipe_title", length = 220)
    private String sourceRecipeTitle;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isBought() {
        return bought;
    }

    public void setBought(boolean bought) {
        this.bought = bought;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getSourceRecipeId() {
        return sourceRecipeId;
    }

    public void setSourceRecipeId(Long sourceRecipeId) {
        this.sourceRecipeId = sourceRecipeId;
    }

    public String getSourceRecipeTitle() {
        return sourceRecipeTitle;
    }

    public void setSourceRecipeTitle(String sourceRecipeTitle) {
        this.sourceRecipeTitle = sourceRecipeTitle;
    }
}
