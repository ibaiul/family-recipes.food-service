package eus.ibai.family.recipes.food.event;

public record PropertyCreatedEvent(String aggregateId, String propertyName) implements DomainEvent<String> {}
