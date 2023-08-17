package eus.ibai.family.recipes.food.wm.domain.recipe;

import reactor.core.publisher.Mono;

import java.util.Set;

public interface RecipeService {

    Mono<String> createRecipe(String recipeName);

    Mono<Void> updateRecipe(String recipeId, String recipeName, Set<String> links);

    Mono<Void> deleteRecipe(String recipeId);

    Mono<String> addRecipeIngredient(String recipeId, String ingredientName);

    Mono<Void> removeRecipeIngredient(String recipeId, String ingredientId);
}
