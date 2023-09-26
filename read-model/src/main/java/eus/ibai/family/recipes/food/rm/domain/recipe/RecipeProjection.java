package eus.ibai.family.recipes.food.rm.domain.recipe;

import java.util.Collections;
import java.util.Set;

public record RecipeProjection(String id, String name, Set<String> links, Set<RecipeIngredientProjection> ingredients, Set<String> tags) {

    public RecipeProjection(String id, String name, Set<String> links) {
        this(id, name, links, Collections.emptySet(), Collections.emptySet());
    }
}
