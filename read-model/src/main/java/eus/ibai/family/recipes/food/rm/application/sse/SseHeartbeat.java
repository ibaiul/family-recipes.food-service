package eus.ibai.family.recipes.food.rm.application.sse;

import eus.ibai.family.recipes.food.event.DomainEvent;

/**
 * Heartbeat should not be a domain message. Will refactor SSE sink to emit generic SSE events rather than DomainEvents.
 */
public record SseHeartbeat(String aggregateId) implements DomainEvent<String> {

    public SseHeartbeat() {
        this(null);
    }
}
