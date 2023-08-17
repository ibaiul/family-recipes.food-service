package eus.ibai.family.recipes.food.rm.domain.recipe;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Optional;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class FindRecipesByQuery {

    private final String ingredientId;

    private final String propertyId;

    public Optional<String> getIngredientId() {
        return Optional.ofNullable(ingredientId);
    }

    public Optional<String> getPropertyId() {
        return Optional.ofNullable(propertyId);
    }
}

