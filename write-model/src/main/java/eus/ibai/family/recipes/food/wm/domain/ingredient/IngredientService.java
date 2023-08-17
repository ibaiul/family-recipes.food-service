package eus.ibai.family.recipes.food.wm.domain.ingredient;

import reactor.core.publisher.Mono;

public interface IngredientService {

    Mono<String> createIngredient(String ingredientName);

    Mono<Void> updateIngredient(String ingredientId, String ingredientName);

    Mono<Void> deleteIngredient(String ingredientId);

    Mono<String> addIngredientProperty(String ingredientId, String propertyName);

    Mono<Void> removeIngredientProperty(String ingredientId, String propertyId);
}
