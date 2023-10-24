package eus.ibai.family.recipes.food.rm.infrastructure.metric;

import eus.ibai.family.recipes.food.event.RecipeTagAddedEvent;
import eus.ibai.family.recipes.food.event.RecipeTagRemovedEvent;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.RecipeEntityRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeTagStatsRecorderTest {

    @Mock
    private RecipeEntityRepository recipeRepository;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private RecipeTagStatsRecorder statsRecorder;

    @BeforeEach
    void beforeEach() {
        when(recipeRepository.findAllTags()).thenReturn(Flux.just("Dessert", "Vegan", "Dessert", "Main Course"));
    }

    @Test
    void should_record_recipe_tag_count_on_startup() {
        statsRecorder.recordInitialStats();

        double recordedTagAmount = meterRegistry.get("recipe.tags").tag("name", "Dessert").gauge().value();
        assertThat(recordedTagAmount).isEqualTo(2.0);
        recordedTagAmount = meterRegistry.get("recipe.tags").tag("name", "Vegan").gauge().value();
        assertThat(recordedTagAmount).isEqualTo(1.0);
        recordedTagAmount = meterRegistry.get("recipe.tags").tag("name", "Main Course").gauge().value();
        assertThat(recordedTagAmount).isEqualTo(1.0);
    }

    @Test
    void should_record_new_tags() {
        statsRecorder.recordInitialStats();
        RecipeTagAddedEvent event = new RecipeTagAddedEvent("recipeId", "Legume");

        statsRecorder.on(event);

        double recordedTagAmount = meterRegistry.get("recipe.tags").tag("name", "Legume").gauge().value();
        assertThat(recordedTagAmount).isEqualTo(1.0);
    }

    @Test
    void should_record_increment_of_existing_tags() {
        statsRecorder.recordInitialStats();
        RecipeTagAddedEvent event = new RecipeTagAddedEvent("recipeId", "Vegan");

        statsRecorder.on(event);

        double recordedTagAmount = meterRegistry.get("recipe.tags").tag("name", "Vegan").gauge().value();
        assertThat(recordedTagAmount).isEqualTo(2.0);
    }

    @Test
    void should_record_decrement_of_existing_tags() {
        statsRecorder.recordInitialStats();
        RecipeTagRemovedEvent event = new RecipeTagRemovedEvent("recipeId", "Vegan");

        statsRecorder.on(event);

        double recordedTagAmount = meterRegistry.get("recipe.tags").tag("name", "Vegan").gauge().value();
        assertThat(recordedTagAmount).isEqualTo(0.0);
    }

    @Test
    void should_ignore_decrement_of_non_recorded_tags() {
        statsRecorder.recordInitialStats();
        RecipeTagRemovedEvent event = new RecipeTagRemovedEvent("recipeId", "NonRecordedTag");

        statsRecorder.on(event);

        Gauge gauge = meterRegistry.find("recipe.tags").tag("name", "NonRecordedTag").gauge();
        assertThat(gauge).isNull();
    }
}
