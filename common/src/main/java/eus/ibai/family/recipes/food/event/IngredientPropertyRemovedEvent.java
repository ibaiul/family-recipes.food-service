package eus.ibai.family.recipes.food.event;

public record IngredientPropertyRemovedEvent(String aggregateId, IngredientProperty ingredientProperty) implements DomainEvent<String> {}
