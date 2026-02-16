package com.glodea.arhome.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import com.glodea.arhome.dto.RecipeCreateRequest;
import com.glodea.arhome.entity.User;
import com.glodea.arhome.repository.UserRepository;
import com.glodea.arhome.service.RecipeService;

@Controller
@RequestMapping("/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final UserRepository userRepository;

    public RecipeController(RecipeService recipeService, UserRepository userRepository) {
        this.recipeService = recipeService;
        this.userRepository = userRepository;
    }

    @GetMapping("/add")
    public String addRecipe(Model model) {
        model.addAttribute("recipe", new RecipeCreateRequest());
        return "add-recipe";
    }

    @PostMapping("/add")
    public String createRecipe(@ModelAttribute("recipe") RecipeCreateRequest recipeRequest,
                               @org.springframework.web.bind.annotation.RequestParam(value = "photo", required = false) MultipartFile photo) {
        User user = userRepository.findByEmail(org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName())
            .orElseThrow();

        String imagePath = null;
        if (photo != null && !photo.isEmpty()) {
            imagePath = savePhoto(photo);
        }

        recipeService.createRecipe(user, recipeRequest, imagePath);
        return "redirect:/profile";
    }

    private String savePhoto(MultipartFile photo) {
        try {
            String originalName = StringUtils.cleanPath(photo.getOriginalFilename());
            String extension = "";
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalName.substring(dotIndex);
            }
            String filename = UUID.randomUUID() + extension;
            Path uploadDir = Paths.get("uploads");
            Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename);
            photo.transferTo(target.toFile());
            return "/uploads/" + filename;
        } catch (Exception ex) {
            return null;
        }
    }
}
