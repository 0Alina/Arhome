package com.glodea.arhome.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.glodea.arhome.entity.GroceryItem;

public interface GroceryItemRepository extends JpaRepository<GroceryItem, Long> {

    List<GroceryItem> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<GroceryItem> findByIdAndUserId(Long id, Long userId);
}
