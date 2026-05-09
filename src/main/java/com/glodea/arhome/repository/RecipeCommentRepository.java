package com.glodea.arhome.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.glodea.arhome.entity.RecipeComment;

public interface RecipeCommentRepository extends JpaRepository<RecipeComment, Long> {
	@Query("select c from RecipeComment c join fetch c.user where c.recipe.id = :recipeId order by c.createdAt desc")
	List<RecipeComment> findByRecipeIdOrderByCreatedAtDesc(@Param("recipeId") Long recipeId);

    Optional<RecipeComment> findByIdAndRecipeId(Long id, Long recipeId);

    Optional<RecipeComment> findTopByRecipeIdAndUserIdOrderByCreatedAtDesc(Long recipeId, Long userId);

	void deleteAllByRecipeId(Long recipeId);

	@Query("""
		select c from RecipeComment c
		join fetch c.user u
		join fetch c.recipe r
		where lower(c.commentText) like lower(concat('%', :query, '%'))
		   or lower(u.fullName) like lower(concat('%', :query, '%'))
		   or lower(u.email) like lower(concat('%', :query, '%'))
		   or lower(r.title) like lower(concat('%', :query, '%'))
		order by c.createdAt desc
	""")
	List<RecipeComment> searchForAdmin(@Param("query") String query);
}
