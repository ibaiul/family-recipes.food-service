package eus.ibai.family.recipes.food.rm.domain.recipe;

import java.util.Set;

public record RecipeProjection(String id, String name, Set<String> links, Set<RecipeIngredientProjection> ingredients, Set<String> tags) {}
