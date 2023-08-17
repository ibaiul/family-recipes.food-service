package eus.ibai.family.recipes.food.rm.application.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import eus.ibai.family.recipes.food.event.DomainEvent;
import eus.ibai.family.recipes.food.event.IngredientCreatedEvent;
import eus.ibai.family.recipes.food.event.RecipeCreatedEvent;
import org.axonframework.eventhandling.GenericEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;

import java.util.ArrayList;
import java.util.List;

import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

class SseEventPublisherTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final int HEARTBEAT_INTERVAL = 100;

    private SseEventPublisher sseEventPublisher;

    static {
        objectMapper.registerSubtypes(new NamedType(RecipeCreatedEvent.class, "RecipeCreatedEvent"));
        objectMapper.registerSubtypes(new NamedType(IngredientCreatedEvent.class, "IngredientCreatedEvent"));
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("eus.ibai.family.recipes")
                .allowIfSubType("java.util.ArrayList")
                .build();
        objectMapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.NON_FINAL);
    }

    @BeforeEach
    void beforeEach() {
        sseEventPublisher = new SseEventPublisher(false, HEARTBEAT_INTERVAL);
        sseEventPublisher.initHeartbeats();
    }

    @Test
    void should_publish_sse_event_when_domain_event_is_received() {
        List<ServerSentEvent<DomainEvent<String>>> subscriberReceivedEvents = new ArrayList<>();
        sseEventPublisher.createSubscription()
                .subscribe(subscriberReceivedEvents::add);
        RecipeCreatedEvent domainEvent = new RecipeCreatedEvent(generateId(), "Paella");
        GenericEventMessage<DomainEvent<String>> axonEvent = new GenericEventMessage<>(domainEvent);

        sseEventPublisher.handle(axonEvent);

        assertAll(
                () -> assertThat(subscriberReceivedEvents).hasSize(1),
                () -> assertThat(subscriberReceivedEvents.get(0)).matches(sseEvent -> sseEvent.id().equals(axonEvent.getIdentifier())),
                () -> assertThat(subscriberReceivedEvents.get(0)).matches(sseEvent -> sseEvent.event().equals(domainEvent.getClass().getSimpleName())),
                () -> assertThat(subscriberReceivedEvents.get(0)).matches(sseEvent -> sseEvent.data().equals(domainEvent))
        );
    }

    @Test
    void should_multicast_received_domain_events_to_all_subscribers() {
        List<DomainEvent<String>> subscriber1ReceivedEvents = new ArrayList<>();
        List<DomainEvent<String>> subscriber2ReceivedEvents = new ArrayList<>();
        sseEventPublisher.createSubscription()
                .subscribe(sseEvent -> subscriber1ReceivedEvents.add(sseEvent.data()));
        sseEventPublisher.createSubscription()
                .subscribe(sseEvent -> subscriber2ReceivedEvents.add(sseEvent.data()));
        RecipeCreatedEvent domainEvent1 = new RecipeCreatedEvent(generateId(), "Paella");
        IngredientCreatedEvent domainEvent2 = new IngredientCreatedEvent(generateId(), "Rice");
        GenericEventMessage<DomainEvent<String>> axonEvent1 = new GenericEventMessage<>(domainEvent1);
        GenericEventMessage<DomainEvent<String>> axonEvent2 = new GenericEventMessage<>(domainEvent2);

        sseEventPublisher.handle(axonEvent1);
        sseEventPublisher.handle(axonEvent2);

        assertThat(subscriber1ReceivedEvents).containsExactly(domainEvent1, domainEvent2);
        assertThat(subscriber2ReceivedEvents).containsExactly(domainEvent1, domainEvent2);
    }

    @Test
    void should_publish_heartbeat_when_enabled() {
        List<SseHeartbeat> subscriberReceivedHeartbeats = new ArrayList<>();
        sseEventPublisher = new SseEventPublisher(true, HEARTBEAT_INTERVAL);
        sseEventPublisher.createSubscription().subscribe(sseEvent -> {
            if (sseEvent.data() instanceof SseHeartbeat heartbeat) {
                subscriberReceivedHeartbeats.add(heartbeat);
            }
        });
        sseEventPublisher.initHeartbeats();

        await().atMost(HEARTBEAT_INTERVAL * 3, MILLISECONDS).pollInterval(50, MILLISECONDS)
                .untilAsserted(() -> assertThat(subscriberReceivedHeartbeats).isNotEmpty());
    }

    @Test
    void should_not_publish_heartbeat_when_disabled() {
        List<SseHeartbeat> subscriberReceivedHeartbeats = new ArrayList<>();
        sseEventPublisher.createSubscription().subscribe(sseEvent -> {
            if (sseEvent.data() instanceof SseHeartbeat heartbeat) {
                subscriberReceivedHeartbeats.add(heartbeat);
            }
        });

        await().during(HEARTBEAT_INTERVAL * 3, MILLISECONDS).pollInterval(50, MILLISECONDS)
                .untilAsserted(() -> assertThat(subscriberReceivedHeartbeats).isEmpty());
    }
}
