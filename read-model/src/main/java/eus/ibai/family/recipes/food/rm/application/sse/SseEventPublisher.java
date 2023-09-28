package eus.ibai.family.recipes.food.rm.application.sse;

import eus.ibai.family.recipes.food.event.DomainEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static eus.ibai.family.recipes.food.rm.infrastructure.config.AxonConfig.SSE_EVENT_PROCESSOR;

@Slf4j
@Component
@ProcessingGroup(SSE_EVENT_PROCESSOR)
public class SseEventPublisher {

    private final boolean heartbeatEnabled;

    private final int heartbeatInterval;

    private final Sinks.Many<ServerSentEvent<DomainEvent<String>>> sink;

    public SseEventPublisher(@Value("${sse.heartbeat.enabled}") boolean heartbeatEnabled, @Value("${sse.heartbeat.interval}") int heartbeatInterval) {
        this.heartbeatEnabled = heartbeatEnabled;
        this.heartbeatInterval = heartbeatInterval;
        this.sink = Sinks.many().multicast().directAllOrNothing();
    }

    @PostConstruct
    void initHeartbeats() {
        if (heartbeatEnabled) {
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                if (sink.currentSubscriberCount() > 0) {
                    handle(new GenericEventMessage<>(new SseHeartbeat()));
                }
            }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
        }
    }

    @EventHandler
    void handle(EventMessage<DomainEvent<String>> event) {
        log.trace("Broadcasting domain event {} to {} subscribers", event.getPayload(), sink.currentSubscriberCount());
        ServerSentEvent<DomainEvent<String>> sseEvent = ServerSentEvent.<DomainEvent<String>>builder()
                .id(event.getIdentifier())
                .event(event.getPayload().type())
                .data(event.getPayload())
                .build();
        sink.emitNext(sseEvent, Sinks.EmitFailureHandler.FAIL_FAST);
    }

    public Flux<ServerSentEvent<DomainEvent<String>>> createSubscription() {
        log.debug("Creating SSE subscription");
        return sink.asFlux()
                .onBackpressureDrop(sseEvent -> log.warn("Dropping event: {}", sseEvent.data()))
                .doOnNext(sse -> log.trace("Publishing event {} - {} - {}", sse.id(), sse.event(), sse.data()))
                .doOnCancel(() -> log.debug("SSE subscription cancelled"))
                .doOnTerminate(() -> log.debug("SSE subscription terminated"))
                .doOnComplete(() -> log.debug("SSE subscription completed"));
    }
}
