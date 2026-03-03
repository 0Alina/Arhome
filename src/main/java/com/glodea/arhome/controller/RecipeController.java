package com.glodea.arhome.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.glodea.arhome.dto.RecipeCreateRequest;
import com.glodea.arhome.dto.RecipeIngredientInput;
import com.glodea.arhome.entity.RecipeIngredient;
import com.glodea.arhome.entity.Recipe;
import com.glodea.arhome.entity.User;
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
    private final CloudinaryService cloudinaryService;

    public RecipeController(
        RecipeService recipeService,
        UserRepository userRepository,
        CloudinaryService cloudinaryService
    ) {
        this.recipeService = recipeService;
        this.userRepository = userRepository;
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
        model.addAttribute("recipe", recipe);
        model.addAttribute("ingredientsLines", formatIngredientLines(recipe));
        model.addAttribute("stepsItems", parseSteps(recipe.getSteps()));
        model.addAttribute("tags", mergeTags(recipe));
        model.addAttribute("authorName", recipe.getUser().getFullName());
        model.addAttribute("authorCategory", recipe.getUser().getCategory());
        return "recipe-detail";
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
}
