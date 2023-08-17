package eus.ibai.family.recipes.food.rm.application.dto;

import eus.ibai.family.recipes.food.rm.domain.ingredient.IngredientProjection;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BasicIngredientDto(@NotNull String id, @NotEmpty String name) {

    public static BasicIngredientDto fromProjection(IngredientProjection ingredient) {
        return new BasicIngredientDto(ingredient.id(), ingredient.name());
    }
}
