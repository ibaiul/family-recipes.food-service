package eus.ibai.family.recipes.food.event;

public record IngredientDeletedEvent(String aggregateId, String ingredientName) implements DomainEvent<String> {}
