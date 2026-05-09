package com.glodea.arhome.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.glodea.arhome.entity.RecipeComment;
import com.glodea.arhome.entity.RecipeRating;
import com.glodea.arhome.entity.Recipe;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.RecipeCommentRepository;
import com.glodea.arhome.repository.RecipeRatingRepository;
import com.glodea.arhome.repository.RecipeRepository;
import com.glodea.arhome.repository.UserRepository;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final int PAGE_SIZE = 8;

    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeCommentRepository recipeCommentRepository;
    private final RecipeRatingRepository recipeRatingRepository;

    public AdminController(
        UserRepository userRepository,
        RecipeRepository recipeRepository,
        RecipeCommentRepository recipeCommentRepository,
        RecipeRatingRepository recipeRatingRepository
    ) {
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
        this.recipeCommentRepository = recipeCommentRepository;
        this.recipeRatingRepository = recipeRatingRepository;
    }

    @GetMapping("/admin")
    public String adminRoot() {
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users")
    public String users(
        @RequestParam(value = "q", required = false) String query,
        @RequestParam(value = "page", required = false) Integer page,
        Model model
    ) {
        String normalizedQuery = normalizeQuery(query);
        Page<User> usersPage = userRepository
            .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByIdDesc(
                normalizedQuery,
                normalizedQuery,
                PageRequest.of(normalizePageIndex(page), PAGE_SIZE)
            );

        addCommonMetrics(model);
        model.addAttribute("searchQuery", normalizedQuery);
        model.addAttribute("sectionTotal", userRepository.count());
        model.addAttribute("users", usersPage.getContent());
        model.addAttribute("currentPage", usersPage.getNumber() + 1);
        model.addAttribute("totalPages", Math.max(usersPage.getTotalPages(), 1));
        return "admin-users";
    }

    @PostMapping("/admin/users/{userId}/restrict")
    public String restrictUser(
        @PathVariable("userId") long userId,
        @RequestParam(value = "q", required = false) String query,
        @RequestParam(value = "page", required = false) Integer page,
        RedirectAttributes redirectAttributes
    ) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("adminError", "User not found.");
            return redirectToUsers(query, page);
        }

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            redirectAttributes.addFlashAttribute("adminError", "Administrator accounts cannot be restricted.");
            return redirectToUsers(query, page);
        }

        if (!user.isRestricted()) {
            user.setRestricted(true);
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("adminMessage", "User has been restricted.");
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", "User is already restricted.");
        }

        return redirectToUsers(query, page);
    }

    @GetMapping("/admin/recipes")
    public String recipes(
        @RequestParam(value = "q", required = false) String query,
        @RequestParam(value = "page", required = false) Integer page,
        Model model
    ) {
        String normalizedQuery = normalizeQuery(query);
        Page<Recipe> recipesPage = recipeRepository
            .findByTitleContainingIgnoreCaseOrUser_FullNameContainingIgnoreCaseOrderByCreatedAtDesc(
                normalizedQuery,
                normalizedQuery,
                PageRequest.of(normalizePageIndex(page), PAGE_SIZE)
            );

        addCommonMetrics(model);
        model.addAttribute("searchQuery", normalizedQuery);
        model.addAttribute("sectionTotal", recipeRepository.count());
        model.addAttribute("recipes", recipesPage.getContent());
        model.addAttribute("currentPage", recipesPage.getNumber() + 1);
        model.addAttribute("totalPages", Math.max(recipesPage.getTotalPages(), 1));
        return "admin-recipes";
    }

    @PostMapping("/admin/recipes/{recipeId}/delete")
    public String deleteRecipe(
        @PathVariable("recipeId") long recipeId,
        @RequestParam(value = "q", required = false) String query,
        @RequestParam(value = "page", required = false) Integer page,
        RedirectAttributes redirectAttributes
    ) {
        if (!recipeRepository.existsById(recipeId)) {
            redirectAttributes.addFlashAttribute("adminError", "Recipe not found.");
            return redirectToRecipes(query, page);
        }

        recipeRatingRepository.deleteAllByRecipeId(recipeId);
        recipeCommentRepository.deleteAllByRecipeId(recipeId);
        recipeRepository.deleteById(recipeId);

        redirectAttributes.addFlashAttribute("adminMessage", "Recipe deleted.");
        return redirectToRecipes(query, page);
    }

    @GetMapping("/admin/reviews")
    public String reviews(
        @RequestParam(value = "q", required = false) String query,
        @RequestParam(value = "page", required = false) Integer page,
        Model model
    ) {
        String normalizedQuery = normalizeQuery(query);
        List<RecipeComment> allComments = recipeCommentRepository.searchForAdmin(normalizedQuery);
        int totalRows = allComments.size();
        int totalPages = Math.max((int) Math.ceil(totalRows / (double) PAGE_SIZE), 1);
        int currentPage = normalizePageNumber(page, totalPages);
        int fromIndex = Math.min((currentPage - 1) * PAGE_SIZE, totalRows);
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalRows);
        List<RecipeComment> comments = allComments.subList(fromIndex, toIndex);
        List<AdminReviewRow> reviewRows = comments.stream()
            .map(this::buildReviewRow)
            .toList();

        addCommonMetrics(model);
        model.addAttribute("searchQuery", normalizedQuery);
        model.addAttribute("sectionTotal", recipeCommentRepository.count());
        model.addAttribute("reviewRows", reviewRows);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("totalPages", totalPages);
        return "admin-reviews";
    }

    @PostMapping("/admin/reviews/{commentId}/delete")
    public String deleteReview(
        @PathVariable("commentId") long commentId,
        @RequestParam(value = "q", required = false) String query,
        @RequestParam(value = "page", required = false) Integer page,
        RedirectAttributes redirectAttributes
    ) {
        RecipeComment comment = recipeCommentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            redirectAttributes.addFlashAttribute("adminError", "Review not found.");
            return redirectToReviews(query, page);
        }

        Long recipeId = comment.getRecipe() != null ? comment.getRecipe().getId() : null;
        Long userId = comment.getUser() != null ? comment.getUser().getId() : null;
        if (recipeId != null && userId != null) {
            recipeRatingRepository.deleteByRecipeIdAndUserId(recipeId, userId);
        }

        recipeCommentRepository.delete(comment);
        redirectAttributes.addFlashAttribute("adminMessage", "Review deleted.");
        return redirectToReviews(query, page);
    }

    private void addCommonMetrics(Model model) {
        model.addAttribute("usersCount", userRepository.count());
        model.addAttribute("recipesCount", recipeRepository.count());
        model.addAttribute("reviewCommentsCount", recipeCommentRepository.count());
        model.addAttribute("reviewRatingsCount", recipeRatingRepository.count());
        model.addAttribute("reviewsCount", recipeCommentRepository.count() + recipeRatingRepository.count());
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        return query.trim();
    }

    private int normalizePageIndex(Integer page) {
        if (page == null || page < 1) {
            return 0;
        }
        return page - 1;
    }

    private int normalizePageNumber(Integer page, int totalPages) {
        if (page == null || page < 1) {
            return 1;
        }
        return Math.min(page, totalPages);
    }

    private AdminReviewRow buildReviewRow(RecipeComment comment) {
        Long recipeId = comment.getRecipe() != null ? comment.getRecipe().getId() : null;
        Long userId = comment.getUser() != null ? comment.getUser().getId() : null;
        BigDecimal ratingValue = null;
        if (recipeId != null && userId != null) {
            ratingValue = recipeRatingRepository.findTopByRecipeIdAndUserIdOrderByUpdatedAtDesc(recipeId, userId)
                .map(RecipeRating::getRatingValue)
                .orElse(null);
        }

        String recipeTitle = comment.getRecipe() != null ? comment.getRecipe().getTitle() : "-";
        String authorName = comment.getUser() != null ? comment.getUser().getFullName() : "-";

        return new AdminReviewRow(
            comment.getId(),
            recipeTitle,
            authorName,
            comment.getCommentText(),
            ratingValue,
            comment.getCreatedAt()
        );
    }

    private String redirectToUsers(String query, Integer page) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/users");
        String normalizedQuery = normalizeQuery(query);
        if (!normalizedQuery.isBlank()) {
            builder.queryParam("q", normalizedQuery);
        }
        if (page != null && page > 0) {
            builder.queryParam("page", page);
        }
        return "redirect:" + builder.build().toUriString();
    }

    private String redirectToRecipes(String query, Integer page) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/recipes");
        String normalizedQuery = normalizeQuery(query);
        if (!normalizedQuery.isBlank()) {
            builder.queryParam("q", normalizedQuery);
        }
        if (page != null && page > 0) {
            builder.queryParam("page", page);
        }
        return "redirect:" + builder.build().toUriString();
    }

    private String redirectToReviews(String query, Integer page) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/reviews");
        String normalizedQuery = normalizeQuery(query);
        if (!normalizedQuery.isBlank()) {
            builder.queryParam("q", normalizedQuery);
        }
        if (page != null && page > 0) {
            builder.queryParam("page", page);
        }
        return "redirect:" + builder.build().toUriString();
    }

    public record AdminReviewRow(
        Long commentId,
        String recipeTitle,
        String authorName,
        String commentText,
        BigDecimal ratingValue,
        Instant createdAt
    ) {
    }
}