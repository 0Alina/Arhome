package com.glodea.arhome.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.glodea.arhome.entity.RecipeComment;

public interface RecipeCommentRepository extends JpaRepository<RecipeComment, Long> {
	List<RecipeComment> findByRecipeIdOrderByCreatedAtDesc(Long recipeId);
}
