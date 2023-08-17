package eus.ibai.family.recipes.food.event;

public record IngredientPropertyAddedEvent(String aggregateId, IngredientProperty ingredientProperty) implements DomainEvent<String> {}
