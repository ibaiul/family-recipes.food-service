package eus.ibai.family.recipes.food.rm.domain.ingredient;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Optional;

@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class FindIngredientsByQuery {

    private final String propertyId;

    public Optional<String> getPropertyId() {
        return Optional.ofNullable(propertyId);
    }
}
