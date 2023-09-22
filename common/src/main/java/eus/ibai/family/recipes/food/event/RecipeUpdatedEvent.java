package eus.ibai.family.recipes.food.event;

import org.axonframework.serialization.Revision;

import java.util.Set;

@Revision("2.0")
public record RecipeUpdatedEvent(String aggregateId, String recipeName, Set<String> recipeLinks) implements DomainEvent<String> {}
