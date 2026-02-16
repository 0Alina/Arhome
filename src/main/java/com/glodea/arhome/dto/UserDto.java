package com.glodea.arhome.dto;

public class UserDto {

    private String fullName;
    private String email;
    private String category;

    public UserDto() {
    }

    public UserDto(String fullName, String email, String category) {
        this.fullName = fullName;
        this.email = email;
        this.category = category;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
