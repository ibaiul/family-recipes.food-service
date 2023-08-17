package eus.ibai.family.recipes.food.event;

public record RecipeCreatedEvent(String aggregateId, String recipeName)  implements DomainEvent<String> {}
