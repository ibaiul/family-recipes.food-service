package eus.ibai.family.recipes.food.event;

import java.time.LocalDateTime;

public record IngredientProperty(String propertyId, LocalDateTime addedOn) {}
