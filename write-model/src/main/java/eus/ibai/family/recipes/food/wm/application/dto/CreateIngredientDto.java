package eus.ibai.family.recipes.food.wm.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateIngredientDto(@NotBlank String ingredientName) {}
