package eus.ibai.family.recipes.food.wm.application.dto;

import eus.ibai.family.recipes.food.wm.application.validation.ValidUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateRecipeDto(@NotBlank String recipeName, @NotNull @ValidUrl Set<String> recipeLinks) {}
