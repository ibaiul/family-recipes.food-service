package eus.ibai.family.recipes.food.rm.domain.ingredient;

import java.time.LocalDateTime;

public record IngredientPropertyProjection(String id, String name, LocalDateTime addedOn) {}
