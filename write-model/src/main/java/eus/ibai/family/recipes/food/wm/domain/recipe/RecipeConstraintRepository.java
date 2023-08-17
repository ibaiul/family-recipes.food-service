package eus.ibai.family.recipes.food.wm.domain.recipe;

import java.util.Optional;

public interface RecipeConstraintRepository {

    boolean nameExists(String recipeName);

    boolean anotherRecipeContainsName(String recipeId, String recipeName);

    Optional<String> retrieveIngredientId(String ingredientName);
}
