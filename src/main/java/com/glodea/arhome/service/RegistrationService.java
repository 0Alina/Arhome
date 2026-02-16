package com.glodea.arhome.service;

import com.glodea.arhome.entity.User;

public interface RegistrationService {
    User register(String fullName, String email, String password, String confirmPassword);
}
