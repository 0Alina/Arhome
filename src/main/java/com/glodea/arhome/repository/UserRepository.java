package com.glodea.arhome.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.glodea.arhome.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByIdDesc(
        String fullName,
        String email,
        Pageable pageable
    );
}
