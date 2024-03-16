package eus.ibai.family.recipes.food.event;

public record RecipeImageRemovedEvent(String aggregateId, String imageId) implements DomainEvent<String> {}
