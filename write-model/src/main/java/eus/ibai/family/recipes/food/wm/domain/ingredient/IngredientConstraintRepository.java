package eus.ibai.family.recipes.food.wm.domain.ingredient;

import java.util.Optional;

public interface IngredientConstraintRepository {

    boolean nameExists(String ingredientName);

    boolean anotherIngredientContainsName(String ingredientId, String ingredientName);

    boolean isIngredientBoundToRecipes(String ingredientId);

    Optional<String> retrievePropertyId(String propertyName);
}
