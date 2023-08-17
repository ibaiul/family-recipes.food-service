package eus.ibai.family.recipes.food.wm.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePropertyDto(@NotBlank String propertyName) {}
