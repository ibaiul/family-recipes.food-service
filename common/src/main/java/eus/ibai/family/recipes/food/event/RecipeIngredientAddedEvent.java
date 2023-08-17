package eus.ibai.family.recipes.food.event;

public record RecipeIngredientAddedEvent(String aggregateId, RecipeIngredient recipeIngredient) implements DomainEvent<String> {}
