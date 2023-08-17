package eus.ibai.family.recipes.food.event;

public record RecipeIngredientRemovedEvent(String aggregateId, RecipeIngredient recipeIngredient) implements DomainEvent<String> {}
