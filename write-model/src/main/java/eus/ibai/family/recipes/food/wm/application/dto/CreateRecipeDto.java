package eus.ibai.family.recipes.food.wm.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRecipeDto(@NotBlank String recipeName) {}
