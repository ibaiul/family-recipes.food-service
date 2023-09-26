package eus.ibai.family.recipes.food.rm.domain.recipe;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.Optional;

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class FindRecipesByQuery {

    private final String ingredientId;

    private final String propertyId;

    private final String tag;

    public Optional<String> getIngredientId() {
        return Optional.ofNullable(ingredientId);
    }

    public Optional<String> getPropertyId() {
        return Optional.ofNullable(propertyId);
    }

    public Optional<String> getTag() {
        return Optional.ofNullable(tag);
    }
}

