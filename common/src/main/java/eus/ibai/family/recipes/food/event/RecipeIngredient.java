package eus.ibai.family.recipes.food.event;

import java.time.LocalDateTime;

public record RecipeIngredient(String ingredientId, LocalDateTime addedOn) {}
