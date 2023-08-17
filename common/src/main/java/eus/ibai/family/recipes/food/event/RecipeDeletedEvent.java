package eus.ibai.family.recipes.food.event;

public record RecipeDeletedEvent(String aggregateId, String recipeName) implements DomainEvent<String> {}
