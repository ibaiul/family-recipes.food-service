package eus.ibai.family.recipes.food.rm.application.dto;

import eus.ibai.family.recipes.food.rm.domain.recipe.RecipeProjection;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

public record RecipeDto(@NotNull String id, @NotEmpty String name, Set<String> links, Set<RecipeIngredientDto> ingredients, Set<String> tags) {

    public static RecipeDto fromProjection(RecipeProjection recipe) {
        Set<RecipeIngredientDto> ingredients = recipe.ingredients().stream()
                .map(RecipeIngredientDto::fromProjection)
                .collect(Collectors.toSet());
        return new RecipeDto(recipe.id(), recipe.name(), recipe.links(), ingredients, recipe.tags());
    }
}

