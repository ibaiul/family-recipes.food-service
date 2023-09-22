package eus.ibai.family.recipes.food.event;

public record RecipeTagRemovedEvent(String aggregateId, String recipeTag) implements DomainEvent<String> {}
