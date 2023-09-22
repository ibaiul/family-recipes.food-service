package eus.ibai.family.recipes.food.event;

public record RecipeTagAddedEvent(String aggregateId, String recipeTag) implements DomainEvent<String> {}
