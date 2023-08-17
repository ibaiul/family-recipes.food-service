package eus.ibai.family.recipes.food.rm.domain.recipe;

import java.time.LocalDateTime;

public record RecipeIngredientProjection(String id, String name, LocalDateTime addedOn) {}
