package com.glodea.arhome.dto;

public class GroceryItemResponse {

    private Long id;
    private String productName;
    private String quantity;
    private String unit;
    private boolean bought;

    public GroceryItemResponse() {
    }

    public GroceryItemResponse(Long id, String productName, String quantity, String unit, boolean bought) {
        this.id = id;
        this.productName = productName;
        this.quantity = quantity;
        this.unit = unit;
        this.bought = bought;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
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
}
