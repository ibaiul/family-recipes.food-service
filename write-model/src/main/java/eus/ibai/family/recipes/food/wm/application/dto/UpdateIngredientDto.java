package eus.ibai.family.recipes.food.wm.application.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateIngredientDto(@NotBlank String ingredientName) {}
