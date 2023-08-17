package eus.ibai.family.recipes.food.event;

public record PropertyUpdatedEvent(String aggregateId, String propertyName) implements DomainEvent<String> {}
