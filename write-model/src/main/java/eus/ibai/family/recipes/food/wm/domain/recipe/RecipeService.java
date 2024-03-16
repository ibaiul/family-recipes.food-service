package eus.ibai.family.recipes.food.wm.domain.recipe;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.Set;

public interface RecipeService {

    Mono<String> createRecipe(String recipeName);

    Mono<Void> updateRecipe(String recipeId, String recipeName, Set<String> links);

    Mono<Void> deleteRecipe(String recipeId);

    Mono<String> addRecipeIngredient(String recipeId, String ingredientName);

    Mono<Void> removeRecipeIngredient(String recipeId, String ingredientId);

    Mono<Void> addRecipeTag(String recipeId, String tag);

    Mono<Void> removeRecipeTag(String recipeId, String tag);

    Mono<String> addRecipeImage(String recipeId, String mediaType, long length, Flux<ByteBuffer> fileContent);

    Mono<Void> removeRecipeImage(String recipeId, String imageId);
}
