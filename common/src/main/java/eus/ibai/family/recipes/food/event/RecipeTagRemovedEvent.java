package eus.ibai.family.recipes.food.event;

import org.axonframework.serialization.Revision;

@Revision("1.0")
public record RecipeTagRemovedEvent(String aggregateId, String recipeTag) implements DomainEvent<String> {}
