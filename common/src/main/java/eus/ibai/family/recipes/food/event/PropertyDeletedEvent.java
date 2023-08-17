package eus.ibai.family.recipes.food.event;

public record PropertyDeletedEvent(String aggregateId, String propertyName) implements DomainEvent<String> {}
