package eus.ibai.family.recipes.food.wm.application.dto;

import jakarta.validation.constraints.NotEmpty;

public record AddRecipeTagDto(@NotEmpty String tag) {}
