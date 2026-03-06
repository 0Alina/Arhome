package com.glodea.arhome.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;

import com.glodea.arhome.dto.RecipeCreateRequest;
import com.glodea.arhome.dto.RecipeIngredientInput;
import com.glodea.arhome.entity.RecipeIngredient;
import com.glodea.arhome.entity.RecipeComment;
import com.glodea.arhome.entity.Recipe;
import com.glodea.arhome.entity.RecipeRating;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.RecipeCommentRepository;
import com.glodea.arhome.repository.RecipeRatingRepository;
import com.glodea.arhome.repository.UserRepository;
import com.glodea.arhome.service.CloudinaryService;
import com.glodea.arhome.service.RecipeService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final UserRepository userRepository;
    private final RecipeCommentRepository recipeCommentRepository;
    private final RecipeRatingRepository recipeRatingRepository;
    private final CloudinaryService cloudinaryService;

    public RecipeController(
        RecipeService recipeService,
        UserRepository userRepository,
        RecipeCommentRepository recipeCommentRepository,
        RecipeRatingRepository recipeRatingRepository,
        CloudinaryService cloudinaryService
    ) {
        this.recipeService = recipeService;
        this.userRepository = userRepository;
        this.recipeCommentRepository = recipeCommentRepository;
        this.recipeRatingRepository = recipeRatingRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/add")
    public String addRecipe(Model model) {
        RecipeCreateRequest request = new RecipeCreateRequest();
        request.setIngredientItems(List.of(new RecipeIngredientInput("", "", "")));
        model.addAttribute("recipe", request);
        model.addAttribute("formAction", "/recipes/add");
        model.addAttribute("pageTitle", "Add Recipe");
        return "add-recipe";
    }

    @PostMapping("/add")
    public String createRecipe(@ModelAttribute("recipe") RecipeCreateRequest recipeRequest,
                               @RequestParam(value = "photo", required = false) MultipartFile photo) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/?authError#auth-login";
        }

        String imagePath = cloudinaryService.uploadImage(photo, "arhome/recipes");

        recipeService.createRecipe(user, recipeRequest, imagePath);
        return "redirect:/profile";
    }

    @PostMapping("/upload-image")
    @ResponseBody
    public java.util.Map<String, Object> uploadImage(@RequestParam("photo") MultipartFile photo) {
        String imagePath = cloudinaryService.uploadImage(photo, "arhome/recipes");
        if (imagePath == null) {
            return java.util.Map.of("ok", false);
        }
        return java.util.Map.of("ok", true, "url", imagePath);
    }

    @GetMapping("/ingredients/suggestions")
    @ResponseBody
    public List<String> ingredientSuggestions(@RequestParam(name = "q", required = false) String query) {
        return recipeService.suggestIngredientNames(query);
    }

    @GetMapping("/{id}/edit")
    public String editRecipe(@PathVariable("id") Long id, Model model) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/?authError#auth-login";
        }

        Recipe recipe = recipeService.getRecipeForUser(user, id);
        RecipeCreateRequest request = new RecipeCreateRequest();
        request.setTitle(recipe.getTitle());
        request.setShortDescription(recipe.getShortDescription());
        request.setPrepTime(recipe.getPrepTime());
        request.setMealType(recipe.getMealType());
        request.setIngredientItems(toIngredientInputs(recipe));
        request.setSteps(recipe.getSteps());
        request.setInstructions(recipe.getInstructions());
        request.setRegionTags(recipe.getRegionTags() != null ? recipe.getRegionTags() : List.of());
        request.setStyleTags(recipe.getStyleTags() != null ? recipe.getStyleTags() : List.of());
        request.setNutritionTags(recipe.getNutritionTags() != null ? recipe.getNutritionTags() : List.of());
        request.setImagePath(recipe.getImagePath());

        model.addAttribute("recipe", request);
        model.addAttribute("formAction", "/recipes/" + id + "/edit");
        model.addAttribute("pageTitle", "Edit Recipe");
        model.addAttribute("isEdit", true);
        return "add-recipe";
    }

    @GetMapping("/{id}")
    public String viewRecipe(@PathVariable("id") Long id, Model model) {
        Recipe recipe = recipeService.getRecipeById(id);
        User currentUser = getCurrentUser();
        boolean isOwnRecipe = currentUser != null
            && currentUser.getId() != null
            && recipe.getUser() != null
            && recipe.getUser().getId() != null
            && recipe.getUser().getId().equals(currentUser.getId());
        model.addAttribute("recipe", recipe);
        model.addAttribute("ingredientsLines", formatIngredientLines(recipe));
        model.addAttribute("stepsItems", parseSteps(recipe.getSteps()));
        model.addAttribute("tags", mergeTags(recipe));
        model.addAttribute("authorName", recipe.getUser().getFullName());
        model.addAttribute("authorCategory", recipe.getUser().getCategory());
        model.addAttribute("recipeAverageRating", recipeService.getAverageRatingForRecipe(id));
        model.addAttribute("recipeRatingCount", recipeService.getRatingCountForRecipe(id));
        model.addAttribute("reviewItems", buildReviewItems(id, currentUser != null ? currentUser.getId() : null));
        boolean hasCurrentUserReview = currentUser != null
            && currentUser.getId() != null
            && recipeCommentRepository.findTopByRecipeIdAndUserIdOrderByCreatedAtDesc(id, currentUser.getId()).isPresent();
        model.addAttribute("hasCurrentUserReview", hasCurrentUserReview);
        model.addAttribute("isOwnRecipe", isOwnRecipe);
        return "recipe-detail";
    }

    @PostMapping("/{id}/review")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> submitReview(
        @PathVariable("id") Long id,
        @RequestBody java.util.Map<String, Object> payload
    ) {
        User user = getCurrentUser();
        if (user == null) {
            return org.springframework.http.ResponseEntity.status(401)
                .body(java.util.Map.of("ok", false, "message", "Authentication required."));
        }

        String mode = payload.get("mode") != null ? payload.get("mode").toString().trim().toLowerCase() : "";

        Double ratingValue = parseDouble(payload.get("rating"));
        String commentText = payload.get("comment") != null ? payload.get("comment").toString().trim() : "";

        try {
            if ("rating".equals(mode)) {
                if (ratingValue == null) {
                    return org.springframework.http.ResponseEntity.badRequest()
                        .body(java.util.Map.of("ok", false, "message", "Invalid rating value."));
                }
                recipeService.saveRecipeRating(user, id, ratingValue);
            } else if ("comment".equals(mode)) {
                if (commentText.isBlank()) {
                    return org.springframework.http.ResponseEntity.badRequest()
                        .body(java.util.Map.of("ok", false, "message", "Comment is required."));
                }
                if (recipeCommentRepository.findTopByRecipeIdAndUserIdOrderByCreatedAtDesc(id, user.getId()).isPresent()) {
                    return org.springframework.http.ResponseEntity.status(409)
                        .body(java.util.Map.of("ok", false, "message", "You already reviewed this recipe. Edit your existing review instead."));
                }
                recipeService.saveRecipeComment(user, id, commentText);
            } else {
                if (ratingValue == null || commentText.isBlank()) {
                    return org.springframework.http.ResponseEntity.badRequest()
                        .body(java.util.Map.of("ok", false, "message", "Both rating and comment are required."));
                }
                if (recipeCommentRepository.findTopByRecipeIdAndUserIdOrderByCreatedAtDesc(id, user.getId()).isPresent()) {
                    return org.springframework.http.ResponseEntity.status(409)
                        .body(java.util.Map.of("ok", false, "message", "You already reviewed this recipe. Edit your existing review instead."));
                }
                recipeService.saveRecipeRating(user, id, ratingValue);
                recipeService.saveRecipeComment(user, id, commentText);
            }
        } catch (IllegalArgumentException ex) {
            return org.springframework.http.ResponseEntity.badRequest()
                .body(java.util.Map.of("ok", false, "message", ex.getMessage()));
        }

        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("ok", true);
        response.put("averageRating", recipeService.getAverageRatingForRecipe(id));
        response.put("ratingCount", recipeService.getRatingCountForRecipe(id));

        if (!commentText.isBlank()) {
            RecipeRating rating = recipeRatingRepository.findByRecipeIdAndUserId(id, user.getId()).orElse(null);
            Long latestCommentId = recipeCommentRepository.findTopByRecipeIdAndUserIdOrderByCreatedAtDesc(id, user.getId())
                .map(RecipeComment::getId)
                .orElse(null);
            response.put("reviewItem", buildReviewItem(user, commentText, rating, user.getId(), latestCommentId));
        }

        return org.springframework.http.ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/review/{commentId}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> updateOwnReviewComment(
        @PathVariable("id") Long recipeId,
        @PathVariable("commentId") Long commentId,
        @RequestBody java.util.Map<String, Object> payload
    ) {
        User user = getCurrentUser();
        if (user == null || user.getId() == null) {
            return org.springframework.http.ResponseEntity.status(401)
                .body(java.util.Map.of("ok", false, "message", "Authentication required."));
        }

        String commentText = payload.get("comment") != null ? payload.get("comment").toString().trim() : "";
        Double ratingValue = parseDouble(payload.get("rating"));
        if (commentText.isBlank()) {
            return org.springframework.http.ResponseEntity.badRequest()
                .body(java.util.Map.of("ok", false, "message", "Comment is required."));
        }

        RecipeComment comment = recipeCommentRepository.findByIdAndRecipeId(commentId, recipeId).orElse(null);
        if (comment == null) {
            return org.springframework.http.ResponseEntity.status(404)
                .body(java.util.Map.of("ok", false, "message", "Review not found."));
        }

        Long ownerId = comment.getUser() != null ? comment.getUser().getId() : null;
        if (ownerId == null || !ownerId.equals(user.getId())) {
            return org.springframework.http.ResponseEntity.status(403)
                .body(java.util.Map.of("ok", false, "message", "You can edit only your own review."));
        }

        comment.setCommentText(commentText);
        recipeCommentRepository.save(comment);

        if (ratingValue != null) {
            recipeService.saveRecipeRating(user, recipeId, ratingValue);
        }

        RecipeRating rating = recipeRatingRepository.findByRecipeIdAndUserId(recipeId, user.getId()).orElse(null);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("ok", true);
        response.put("reviewItem", buildReviewItem(user, commentText, rating, user.getId(), comment.getId()));
        response.put("averageRating", recipeService.getAverageRatingForRecipe(recipeId));
        response.put("ratingCount", recipeService.getRatingCountForRecipe(recipeId));
        return org.springframework.http.ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/review/{commentId}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> deleteOwnReviewComment(
        @PathVariable("id") Long recipeId,
        @PathVariable("commentId") Long commentId
    ) {
        User user = getCurrentUser();
        if (user == null || user.getId() == null) {
            return org.springframework.http.ResponseEntity.status(401)
                .body(java.util.Map.of("ok", false, "message", "Authentication required."));
        }

        RecipeComment comment = recipeCommentRepository.findByIdAndRecipeId(commentId, recipeId).orElse(null);
        if (comment == null) {
            return org.springframework.http.ResponseEntity.status(404)
                .body(java.util.Map.of("ok", false, "message", "Review not found."));
        }

        Long ownerId = comment.getUser() != null ? comment.getUser().getId() : null;
        if (ownerId == null || !ownerId.equals(user.getId())) {
            return org.springframework.http.ResponseEntity.status(403)
                .body(java.util.Map.of("ok", false, "message", "You can delete only your own review."));
        }

        recipeCommentRepository.delete(comment);
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of("ok", true, "deletedCommentId", commentId));
    }

    @PostMapping("/{id}/edit")
    public String updateRecipe(@PathVariable("id") Long id,
                               @ModelAttribute("recipe") RecipeCreateRequest recipeRequest,
                               @RequestParam(value = "photo", required = false) MultipartFile photo) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/?authError#auth-login";
        }

        String imagePath = cloudinaryService.uploadImage(photo, "arhome/recipes");

        recipeService.updateRecipe(user, id, recipeRequest, imagePath);
        return "redirect:/profile";
    }

    @PostMapping("/{id}/delete")
    public String deleteRecipe(@PathVariable("id") Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/?authError#auth-login";
        }
        recipeService.deleteRecipe(user, id);
        return "redirect:/profile";
    }

    @PostMapping("/{id}/favorite")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> toggleFavorite(@PathVariable("id") Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return org.springframework.http.ResponseEntity.status(401)
                .body(java.util.Map.of("ok", false));
        }
        boolean favorited = recipeService.toggleFavorite(user, id);
        return org.springframework.http.ResponseEntity.ok(java.util.Map.of("ok", true, "favorited", favorited));
    }

    // Local file storage removed; using Cloudinary.

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email = resolveEmail(authentication);
        if (email == null || email.isBlank()) {
            return null;
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    private String resolveEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.glodea.arhome.config.CustomUserDetails) {
            return ((com.glodea.arhome.config.CustomUserDetails) principal).getUsername();
        }
        if (principal instanceof OAuth2User) {
            Object email = ((OAuth2User) principal).getAttribute("email");
            if (email != null) {
                return email.toString().trim().toLowerCase();
            }
        }
        String fallback = authentication.getName();
        return fallback != null ? fallback.trim().toLowerCase() : null;
    }

    private List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split("\\r?\\n"))
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .toList();
    }

    private Double parseDouble(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<RecipeIngredientInput> toIngredientInputs(Recipe recipe) {
        if (recipe.getRecipeIngredients() != null && !recipe.getRecipeIngredients().isEmpty()) {
            return recipe.getRecipeIngredients().stream()
                .sorted(java.util.Comparator.comparing(RecipeIngredient::getPositionIndex, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(item -> new RecipeIngredientInput(
                    item.getIngredient() != null ? item.getIngredient().getName() : "",
                    item.getQuantity() != null ? item.getQuantity().stripTrailingZeros().toPlainString() : "",
                    item.getUnit()
                ))
                .toList();
        }

        List<RecipeIngredientInput> legacy = splitLines(recipe.getIngredients()).stream()
            .map(this::parseLegacyIngredientLine)
            .toList();

        return legacy.isEmpty() ? List.of(new RecipeIngredientInput("", "", "")) : legacy;
    }

    private RecipeIngredientInput parseLegacyIngredientLine(String line) {
        if (line == null || line.isBlank()) {
            return new RecipeIngredientInput("", "", "");
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
            .compile("^([0-9]+(?:[\\.,][0-9]+)?)\\s+([^\\s]+)\\s+(.+)$")
            .matcher(line.trim());
        if (matcher.matches()) {
            return new RecipeIngredientInput(
                matcher.group(3).trim(),
                matcher.group(1).replace(',', '.'),
                matcher.group(2).trim()
            );
        }
        return new RecipeIngredientInput(line.trim(), "", "");
    }

    private List<String> formatIngredientLines(Recipe recipe) {
        if (recipe.getRecipeIngredients() != null && !recipe.getRecipeIngredients().isEmpty()) {
            return recipe.getRecipeIngredients().stream()
                .sorted(java.util.Comparator.comparing(RecipeIngredient::getPositionIndex, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(item -> {
                    String quantity = item.getQuantity() != null ? item.getQuantity().stripTrailingZeros().toPlainString() : "";
                    String unit = item.getUnit() != null ? item.getUnit().trim() : "";
                    String ingredientName = item.getIngredient() != null ? item.getIngredient().getName() : "";
                    return (quantity + " " + unit + " " + ingredientName).trim();
                })
                .filter(line -> !line.isBlank())
                .toList();
        }

        return splitLines(recipe.getIngredients());
    }

    private List<java.util.Map<String, Object>> parseSteps(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        java.util.Map<String, Object> current = null;

        for (String rawLine : value.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }

            java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("^(\\d+)\\.\\s*(.+)$")
                .matcher(line);

            if (matcher.matches()) {
                current = new java.util.HashMap<>();
                current.put("number", Integer.parseInt(matcher.group(1)));
                current.put("title", matcher.group(2));
                current.put("desc", new java.util.ArrayList<String>());
                result.add(current);
            } else if (current != null) {
                @SuppressWarnings("unchecked")
                List<String> desc = (List<String>) current.get("desc");
                desc.add(line);
            }
        }

        return result;
    }

    private List<String> mergeTags(Recipe recipe) {
        List<String> tags = new java.util.ArrayList<>();
        if (recipe.getRegionTags() != null) {
            tags.addAll(recipe.getRegionTags());
        }
        if (recipe.getStyleTags() != null) {
            tags.addAll(recipe.getStyleTags());
        }
        if (recipe.getNutritionTags() != null) {
            tags.addAll(recipe.getNutritionTags());
        }
        return tags;
    }

    private List<java.util.Map<String, Object>> buildReviewItems(Long recipeId, Long currentUserId) {
        if (recipeId == null) {
            return List.of();
        }

        List<RecipeComment> comments = prioritizeOwnComments(
            keepLatestCommentPerUser(recipeCommentRepository.findByRecipeIdOrderByCreatedAtDesc(recipeId)),
            currentUserId
        );
        if (comments.isEmpty()) {
            return List.of();
        }

        java.util.Map<Long, RecipeRating> ratingsByUser = recipeRatingRepository.findByRecipeId(recipeId)
            .stream()
            .filter(rating -> rating.getUser() != null && rating.getUser().getId() != null)
            .collect(java.util.stream.Collectors.toMap(
                rating -> rating.getUser().getId(),
                rating -> rating,
                (left, right) -> right
            ));

        return comments.stream()
            .map(comment -> {
                User author = comment.getUser();
                RecipeRating rating = author != null && author.getId() != null
                    ? ratingsByUser.get(author.getId())
                    : null;
                return buildReviewItem(author, comment.getCommentText(), rating, currentUserId, comment.getId());
            })
            .toList();
    }

    private List<RecipeComment> prioritizeOwnComments(List<RecipeComment> comments, Long currentUserId) {
        if (comments == null || comments.isEmpty() || currentUserId == null) {
            return comments != null ? comments : List.of();
        }

        List<RecipeComment> ownComments = new java.util.ArrayList<>();
        List<RecipeComment> otherComments = new java.util.ArrayList<>();

        for (RecipeComment comment : comments) {
            Long authorId = comment != null && comment.getUser() != null ? comment.getUser().getId() : null;
            if (authorId != null && authorId.equals(currentUserId)) {
                ownComments.add(comment);
            } else {
                otherComments.add(comment);
            }
        }

        if (ownComments.isEmpty()) {
            return comments;
        }

        List<RecipeComment> ordered = new java.util.ArrayList<>(comments.size());
        ordered.addAll(ownComments);
        ordered.addAll(otherComments);
        return ordered;
    }

    private List<RecipeComment> keepLatestCommentPerUser(List<RecipeComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return comments != null ? comments : List.of();
        }

        java.util.Set<Long> seenUserIds = new java.util.HashSet<>();
        List<RecipeComment> unique = new java.util.ArrayList<>();

        for (RecipeComment comment : comments) {
            Long userId = comment != null && comment.getUser() != null ? comment.getUser().getId() : null;
            if (userId == null) {
                unique.add(comment);
                continue;
            }
            if (seenUserIds.add(userId)) {
                unique.add(comment);
            }
        }

        return unique;
    }

    private java.util.Map<String, Object> buildReviewItem(
        User author,
        String commentText,
        RecipeRating rating,
        Long currentUserId,
        Long commentId
    ) {
        java.util.Map<String, Object> item = new java.util.HashMap<>();
        Long authorId = author != null ? author.getId() : null;
        String name = author != null && author.getFullName() != null && !author.getFullName().isBlank()
            ? author.getFullName()
            : "Anonymous";
        String category = normalizeReviewCategory(author != null ? author.getCategory() : null);
        item.put("userName", name);
        item.put("userCategory", category);
        item.put("starText", toStars(rating));
        item.put("commentText", commentText != null ? commentText : "");
        item.put("commentId", commentId);
        item.put("ownReview", currentUserId != null && authorId != null && currentUserId.equals(authorId));
        return item;
    }

    private String toStars(RecipeRating rating) {
        if (rating == null || rating.getRatingValue() == null) {
            return "";
        }
        int rounded = (int) Math.round(rating.getRatingValue().doubleValue());
        int stars = Math.max(0, Math.min(5, rounded));
        return "⭐".repeat(stars);
    }

    private String normalizeReviewCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank() || "Unselected".equalsIgnoreCase(rawCategory)) {
            return "Young & Hungry";
        }

        String normalized = rawCategory.trim().toLowerCase();
        normalized = normalized.replaceAll("\\s*\\(.*\\)\\s*$", "");
        normalized = normalized.replaceAll("\\s*&\\s*", " & ");
        normalized = normalized.replaceAll("[_-]+", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();

        if (normalized.isBlank()) {
            return "Young & Hungry";
        }

        String compact = normalized.replace(" ", "");
        return switch (compact) {
            case "young&hungry" -> "Young & Hungry";
            case "grandma'sclassics", "grandmasclassics" -> "Grandma's Classics";
            case "homecooks" -> "Home Cooks";
            default -> toTitleCase(normalized);
        };
    }

    private String toTitleCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        boolean capitalizeNext = true;
        for (char ch : value.toCharArray()) {
            if (Character.isLetter(ch)) {
                if (capitalizeNext) {
                    builder.append(Character.toUpperCase(ch));
                    capitalizeNext = false;
                } else {
                    builder.append(Character.toLowerCase(ch));
                }
            } else {
                builder.append(ch);
                capitalizeNext = ch == ' ' || ch == '&' || ch == '\'';
            }
        }
        return builder.toString().trim();
    }
}
