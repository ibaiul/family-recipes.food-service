package eus.ibai.family.recipes.food.event;

import java.util.Set;

public record RecipeUpdatedEvent(String aggregateId, String recipeName, Set<String> recipeLinks) implements DomainEvent<String> {}
