package eus.ibai.family.recipes.food.rm.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import eus.ibai.family.recipes.food.rm.domain.ingredient.IngredientPropertyProjection;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record IngredientPropertyDto(@NotNull String id, @NotEmpty String name, @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS") LocalDateTime addedOn) {

    public static IngredientPropertyDto fromProjection(IngredientPropertyProjection ingredientProperty) {
        return new IngredientPropertyDto(ingredientProperty.id(), ingredientProperty.name(), ingredientProperty.addedOn());
    }
}
