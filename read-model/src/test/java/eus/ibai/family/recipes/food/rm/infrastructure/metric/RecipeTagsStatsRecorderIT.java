package eus.ibai.family.recipes.food.rm.infrastructure.metric;

import eus.ibai.family.recipes.food.event.DomainEvent;
import eus.ibai.family.recipes.food.event.RecipeTagAddedEvent;
import eus.ibai.family.recipes.food.event.RecipeTagRemovedEvent;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.GenericEventMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static eus.ibai.family.recipes.food.test.TestUtils.execute;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
class RecipeTagsStatsRecorderIT {

    @Autowired
    private EventBus eventBus;

    @SpyBean
    RecipeTagStatsRecorder statsRecorder;

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
}
