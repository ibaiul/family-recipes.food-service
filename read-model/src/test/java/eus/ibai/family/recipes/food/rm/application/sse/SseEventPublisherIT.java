package eus.ibai.family.recipes.food.rm.application.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import eus.ibai.family.recipes.food.event.*;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static eus.ibai.family.recipes.food.test.TestUtils.fixedTime;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SseEventPublisherIT {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EventBus eventBus;

    @Autowired
    private SseEventPublisher sseEventPublisher;

    @ParameterizedTest
    @MethodSource
    void should_listen_to_domain_events_and_broadcast_them(DomainEvent<String> domainEvent) {
        List<DomainEvent<String>> subscriberReceivedEvents = new ArrayList<>();

        sseEventPublisher.createSubscription()
                .subscribe(sse -> subscriberReceivedEvents.add(sse.data()));
        EventMessage<DomainEvent<String>> axonEvent = new GenericEventMessage<>(domainEvent);


        eventBus.publish(axonEvent);

        await().atMost(2, SECONDS).untilAsserted(() ->
                assertThat(subscriberReceivedEvents).containsExactly(domainEvent));
    }

    private static Stream<DomainEvent<String>> should_listen_to_domain_events_and_broadcast_them() {
        return Stream.of(
                new RecipeCreatedEvent(generateId(), "Avocado toast"),
                new RecipeUpdatedEvent(generateId(), "Avocado toast", Set.of("https://avocado.com")),
                new RecipeIngredientAddedEvent(generateId(), new RecipeIngredient(generateId(), fixedTime())),
                new RecipeIngredientRemovedEvent(generateId(), new RecipeIngredient(generateId(), fixedTime())),
                new RecipeTagAddedEvent(generateId(), "First course"),
                new RecipeTagRemovedEvent(generateId(), "First course"),
                new RecipeDeletedEvent(generateId(), "Avocado toast"),
                new IngredientCreatedEvent(generateId(), "Avocado"),
                new IngredientUpdatedEvent(generateId(), "Avocado"),
                new IngredientPropertyAddedEvent(generateId(), new IngredientProperty(generateId(), fixedTime())),
                new IngredientPropertyRemovedEvent(generateId(), new IngredientProperty(generateId(), fixedTime())),
                new IngredientDeletedEvent(generateId(), "Avocado"),
                new PropertyCreatedEvent(generateId(), "Antioxidants"),
                new PropertyUpdatedEvent(generateId(), "Antioxidants"),
                new PropertyDeletedEvent(generateId(), "Antioxidants")
        );
    }
}
