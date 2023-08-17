package eus.ibai.family.recipes.food.wm.infrastructure.metric;

import eus.ibai.family.recipes.food.event.DomainEvent;
import eus.ibai.family.recipes.food.event.RecipeCreatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.GenericEventMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static eus.ibai.family.recipes.food.test.TestUtils.execute;
import static eus.ibai.family.recipes.food.wm.infrastructure.metric.FoodStatsRecorder.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
class FoodStatsRecorderIT {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private EventBus eventBus;

    @Test
    void should_intercept_event_messages() {
        RecipeCreatedEvent recipeCreatedEvent = new RecipeCreatedEvent("id", "name");
        GenericEventMessage<DomainEvent<String>> eventMessage = new GenericEventMessage<>(recipeCreatedEvent);
        double initialRecipeAmount = meterRegistry.get(FOOD_ENTITY_METRIC_NAME).tag(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_RECIPES).gauge().value();

        execute(() -> eventBus.publish(eventMessage));

        await().atMost(3, SECONDS).untilAsserted(() -> {
            double recipeAmount = meterRegistry.get(FOOD_ENTITY_METRIC_NAME).tag(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_RECIPES).gauge().value();
            assertThat(recipeAmount).isEqualTo(initialRecipeAmount + 1);
        });
    }
}
