package eus.ibai.family.recipes.food.rm.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import eus.ibai.family.recipes.food.rm.domain.recipe.RecipeIngredientProjection;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RecipeIngredientDto(@NotNull String id, @NotEmpty String name, @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS") @NotNull LocalDateTime addedOn) {

    public static RecipeIngredientDto fromProjection(RecipeIngredientProjection ingredient) {
        return new RecipeIngredientDto(ingredient.id(), ingredient.name(), ingredient.addedOn());
    }
}
