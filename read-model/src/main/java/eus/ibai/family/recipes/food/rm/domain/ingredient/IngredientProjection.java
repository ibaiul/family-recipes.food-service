package eus.ibai.family.recipes.food.rm.domain.ingredient;

import java.util.Collections;
import java.util.Set;

public record IngredientProjection(String id, String name, Set<IngredientPropertyProjection> properties) {

    public IngredientProjection(String id, String name) {
        this(id, name, Collections.emptySet());
    }
}
