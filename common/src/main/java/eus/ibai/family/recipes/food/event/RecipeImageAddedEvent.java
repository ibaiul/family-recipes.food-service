package eus.ibai.family.recipes.food.event;

public record RecipeImageAddedEvent(String aggregateId, String imageId) implements DomainEvent<String> {}
