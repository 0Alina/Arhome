package com.glodea.arhome.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.UserRepository;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(String fullName, String email, String password, String confirmPassword) {
        if (!StringUtils.hasText(fullName) || !StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Missing required fields");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setCategory("Unselected");

        return userRepository.save(user);
    }
}
