package eus.ibai.family.recipes.food.event;

public record IngredientCreatedEvent(String aggregateId, String ingredientName) implements DomainEvent<String> {}
