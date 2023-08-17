package eus.ibai.family.recipes.food.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface DomainEvent<T> {

    T aggregateId();

    @JsonProperty("type")
    default String type() {
        return getClass().getSimpleName();
    }
}
