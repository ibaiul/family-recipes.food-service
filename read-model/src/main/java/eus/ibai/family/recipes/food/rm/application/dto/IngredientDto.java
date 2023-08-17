package eus.ibai.family.recipes.food.rm.application.dto;

import eus.ibai.family.recipes.food.rm.domain.ingredient.IngredientProjection;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

public record IngredientDto(@NotNull String id, @NotEmpty String name, Set<IngredientPropertyDto> properties) {

    public static IngredientDto fromProjection(IngredientProjection ingredient) {
        Set<IngredientPropertyDto> propertyDtos = ingredient.properties().stream()
                .map(IngredientPropertyDto::fromProjection)
                .collect(Collectors.toSet());
        return new IngredientDto(ingredient.id(), ingredient.name(), propertyDtos);
    }
}
