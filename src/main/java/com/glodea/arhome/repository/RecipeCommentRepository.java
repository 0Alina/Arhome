package com.glodea.arhome.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.glodea.arhome.entity.RecipeComment;

public interface RecipeCommentRepository extends JpaRepository<RecipeComment, Long> {
	@Query("select c from RecipeComment c join fetch c.user where c.recipe.id = :recipeId order by c.createdAt desc")
	List<RecipeComment> findByRecipeIdOrderByCreatedAtDesc(@Param("recipeId") Long recipeId);
}
