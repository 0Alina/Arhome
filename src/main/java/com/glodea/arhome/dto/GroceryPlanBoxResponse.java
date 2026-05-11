package com.glodea.arhome.dto;

import java.util.List;

public class GroceryPlanBoxResponse {

    private Long planId;
    private String planTitle;
    private String day;
    private List<GroceryItemResponse> items;

    public GroceryPlanBoxResponse() {
    }

    public GroceryPlanBoxResponse(Long planId, String planTitle, String day, List<GroceryItemResponse> items) {
        this.planId = planId;
        this.planTitle = planTitle;
        this.day = day;
        this.items = items;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getPlanTitle() {
        return planTitle;
    }

    public void setPlanTitle(String planTitle) {
        this.planTitle = planTitle;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public List<GroceryItemResponse> getItems() {
        return items;
    }

    public void setItems(List<GroceryItemResponse> items) {
        this.items = items;
    }
}
