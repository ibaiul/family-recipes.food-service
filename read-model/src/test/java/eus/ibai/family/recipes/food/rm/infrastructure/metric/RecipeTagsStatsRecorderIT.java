package eus.ibai.family.recipes.food.rm.infrastructure.metric;

import eus.ibai.family.recipes.food.event.DomainEvent;
import eus.ibai.family.recipes.food.event.RecipeTagAddedEvent;
import eus.ibai.family.recipes.food.event.RecipeTagRemovedEvent;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static eus.ibai.family.recipes.food.rm.infrastructure.config.AxonConfig.RECIPE_TAG_METRICS_EVENT_PROCESSOR;
import static eus.ibai.family.recipes.food.test.TestUtils.execute;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
class RecipeTagsStatsRecorderIT {

    @Value("${axon.eventhandling.custom-processors.recipe-tag-metrics-event-processor.event-availability-timeout-seconds}")
    private int eventAvailabilityTimeout;

    @SpyBean
    private TokenStore tokenStore;

    @Autowired
    private EventBus eventBus;

    @SpyBean
    private RecipeTagStatsRecorder statsRecorder;

    @Test
    void should_handle_recipe_tag_added_event() {
        RecipeTagAddedEvent event = new RecipeTagAddedEvent("recipeId", "Vegan");
        GenericEventMessage<DomainEvent<String>> eventMessage = new GenericEventMessage<>(event);

        execute(() -> eventBus.publish(eventMessage));

        await().atMost(5, SECONDS).untilAsserted(() -> verify(statsRecorder).on(event));
    }

    @Test
    void should_handle_recipe_tag_removed_event() {
        RecipeTagRemovedEvent event = new RecipeTagRemovedEvent("recipeId", "Vegan");
        GenericEventMessage<DomainEvent<String>> eventMessage = new GenericEventMessage<>(event);

        execute(() -> eventBus.publish(eventMessage));

        await().atMost(5, SECONDS).untilAsserted(() -> verify(statsRecorder).on(event));
    }

    @Test
    void should_wait_the_configured_interval_before_tep_instance_extends_the_token_claim() {
        await().atLeast(eventAvailabilityTimeout - 1, SECONDS).atMost(eventAvailabilityTimeout * 2L, SECONDS)
                .untilAsserted(() -> verify(tokenStore, times(1)).extendClaim(eq(RECIPE_TAG_METRICS_EVENT_PROCESSOR), anyInt()));
    }
}
