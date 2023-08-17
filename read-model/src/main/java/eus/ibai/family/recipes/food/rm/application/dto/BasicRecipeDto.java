package eus.ibai.family.recipes.food.rm.application.dto;

import eus.ibai.family.recipes.food.rm.domain.recipe.RecipeProjection;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BasicRecipeDto(@NotNull String id, @NotEmpty String name) {

    public static BasicRecipeDto fromProjection(RecipeProjection recipe) {
        return new BasicRecipeDto(recipe.id(), recipe.name());
    }
}

