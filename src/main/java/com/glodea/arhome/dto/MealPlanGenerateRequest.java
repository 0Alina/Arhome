package com.glodea.arhome.dto;

import java.util.List;

public class MealPlanGenerateRequest {

    private String source;
    private DietContext diet;
    private CustomContext custom;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public DietContext getDiet() {
        return diet;
    }

    public void setDiet(DietContext diet) {
        this.diet = diet;
    }

    public CustomContext getCustom() {
        return custom;
    }

    public void setCustom(CustomContext custom) {
        this.custom = custom;
    }

    public static class DietContext {

        private String key;
        private String title;
        private List<String> macros;
        private List<String> targets;
        private List<String> eats;
        private List<String> restrictions;
        private String note;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<String> getMacros() {
            return macros;
        }

        public void setMacros(List<String> macros) {
            this.macros = macros;
        }

        public List<String> getTargets() {
            return targets;
        }

        public void setTargets(List<String> targets) {
            this.targets = targets;
        }

        public List<String> getEats() {
            return eats;
        }

        public void setEats(List<String> eats) {
            this.eats = eats;
        }

        public List<String> getRestrictions() {
            return restrictions;
        }

        public void setRestrictions(List<String> restrictions) {
            this.restrictions = restrictions;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    public static class CustomContext {

        private List<String> goals;
        private List<String> allergies;
        private List<String> dietaryPreferences;
        private List<String> enjoyFoods;
        private List<String> avoidFoods;
        private List<String> regions;
        private List<String> times;
        private List<String> styles;
        private List<String> mealsPerDay;

        public List<String> getGoals() {
            return goals;
        }

        public void setGoals(List<String> goals) {
            this.goals = goals;
        }

        public List<String> getAllergies() {
            return allergies;
        }

        public void setAllergies(List<String> allergies) {
            this.allergies = allergies;
        }

        public List<String> getDietaryPreferences() {
            return dietaryPreferences;
        }

        public void setDietaryPreferences(List<String> dietaryPreferences) {
            this.dietaryPreferences = dietaryPreferences;
        }

        public List<String> getEnjoyFoods() {
            return enjoyFoods;
        }

        public void setEnjoyFoods(List<String> enjoyFoods) {
            this.enjoyFoods = enjoyFoods;
        }

        public List<String> getAvoidFoods() {
            return avoidFoods;
        }

        public void setAvoidFoods(List<String> avoidFoods) {
            this.avoidFoods = avoidFoods;
        }

        public List<String> getRegions() {
            return regions;
        }

        public void setRegions(List<String> regions) {
            this.regions = regions;
        }

        public List<String> getTimes() {
            return times;
        }

        public void setTimes(List<String> times) {
            this.times = times;
        }

        public List<String> getStyles() {
            return styles;
        }

        public void setStyles(List<String> styles) {
            this.styles = styles;
        }

        public List<String> getMealsPerDay() {
            return mealsPerDay;
        }

        public void setMealsPerDay(List<String> mealsPerDay) {
            this.mealsPerDay = mealsPerDay;
        }
    }
}
