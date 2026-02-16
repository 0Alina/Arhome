package com.glodea.arhome.service;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.UserRepository;
import com.glodea.arhome.config.CustomUserDetails;

@Service
public class UserDetailsServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new CustomUserDetails(
            user.getEmail(),
            user.getPasswordHash(),
            user.getFullName(),
            user.getCategory(),
            List.of(() -> "ROLE_" + user.getRole())
        );
    }
}
