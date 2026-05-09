package com.glodea.arhome.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.UserRepository;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oauthUser = delegate.loadUser(userRequest);

        Map<String, Object> attributes = oauthUser.getAttributes();
        String email = normalizeEmail(attributes.get("email"));
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Missing email from Google account");
        }

        String fullName = normalizeName(attributes.get("name"));
        if (fullName == null || fullName.isBlank()) {
            String givenName = normalizeName(attributes.get("given_name"));
            String familyName = normalizeName(attributes.get("family_name"));
            if (givenName != null && familyName != null) {
                fullName = (givenName + " " + familyName).trim();
            } else if (givenName != null) {
                fullName = givenName;
            } else if (familyName != null) {
                fullName = familyName;
            }
        }

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setFullName(fullName != null && !fullName.isBlank() ? fullName : email);
            user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
            userRepository.save(user);
        } else if (user.isRestricted()) {
            throw new OAuth2AuthenticationException("Account is restricted");
        } else if (fullName != null && !fullName.isBlank() && !fullName.equals(user.getFullName())) {
            user.setFullName(fullName);
            userRepository.save(user);
        }

        attributes.put("fullName", user.getFullName());
        attributes.put("category", user.getCategory());

        return new DefaultOAuth2User(
            java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())),
            attributes,
            "email"
        );
    }

    private String normalizeEmail(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().trim().toLowerCase();
    }

    private String normalizeName(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().trim();
    }
}
