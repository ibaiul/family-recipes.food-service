package eus.ibai.family.recipes.food.wm.infrastructure.metric;

import eus.ibai.family.recipes.food.event.*;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.IngredientNameConstraintRepository;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.PropertyNameConstraintRepository;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.RecipeNameConstraintRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static eus.ibai.family.recipes.food.test.TestUtils.execute;
import static eus.ibai.family.recipes.food.test.TestUtils.fixedTime;
import static eus.ibai.family.recipes.food.wm.infrastructure.metric.FoodStatsRecorder.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodStatsRecorderTest {

    @Mock
    private RecipeNameConstraintRepository recipeRepository;

    @Mock
    private IngredientNameConstraintRepository ingredientRepository;

    @Mock
    private PropertyNameConstraintRepository propertyRepository;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private FoodStatsRecorder foodStatsRecorder;

    @BeforeEach
    void beforeEach() {
        when(recipeRepository.count()).thenReturn(10L);
        when(ingredientRepository.count()).thenReturn(20L);
        when(propertyRepository.count()).thenReturn(5L);
    }

    @Test
    void should_record_food_entity_count_on_startup() {
        foodStatsRecorder.recordInitialStats();

        double recordedRecipeAmount = meterRegistry.get(FOOD_ENTITY_METRIC_NAME).tag(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_RECIPES).gauge().value();
        assertThat(recordedRecipeAmount).isEqualTo(10.0);
        double recordedIngredientAmount = meterRegistry.get(FOOD_ENTITY_METRIC_NAME).tag(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_INGREDIENTS).gauge().value();
        assertThat(recordedIngredientAmount).isEqualTo(20.0);
        double recordedPropertyAmount = meterRegistry.get(FOOD_ENTITY_METRIC_NAME).tag(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_PROPERTIES).gauge().value();
        assertThat(recordedPropertyAmount).isEqualTo(5.0);
    }

    @ParameterizedTest
    @MethodSource
    void should_record_updated_food_entity_count_when_intercepting_a_domain_event(DomainEvent<String> domainEvent, double expectedRecipeCount,
                                                                            double expectedIngredientCount, double expectedPropertyCount) throws Exception {
        foodStatsRecorder.recordInitialStats();
        InterceptorChain mockInterceptorChain = mock(InterceptorChain.class);
        when(mockInterceptorChain.proceed()).thenReturn(null);
        GenericEventMessage<DomainEvent<String>> axonMessage = new GenericEventMessage<>(domainEvent);
        UnitOfWork<EventMessage<?>> unitOfWork = new DefaultUnitOfWork<>(axonMessage);

        execute(() -> {
            unitOfWork.start();
            foodStatsRecorder.handle(unitOfWork, mockInterceptorChain);
            unitOfWork.commit();
        });

        await().atMost(2, SECONDS).untilAsserted(() -> {
            double recordedRecipeAmount = meterRegistry.get(FOOD_ENTITY_METRIC_NAME).tag(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_RECIPES).gauge().value();
            assertThat(recordedRecipeAmount).isEqualTo(expectedRecipeCount);
            double recordedIngredientAmount = meterRegistry.get(FOOD_ENTITY_METRIC_NAME).tag(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_INGREDIENTS).gauge().value();
            assertThat(recordedIngredientAmount).isEqualTo(expectedIngredientCount);
            double recordedPropertyAmount = meterRegistry.get(FOOD_ENTITY_METRIC_NAME).tag(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_PROPERTIES).gauge().value();
            assertThat(recordedPropertyAmount).isEqualTo(expectedPropertyCount);
        });
    }

    @Test
    void should_discard_non_domain_events() throws Exception {
        foodStatsRecorder.recordInitialStats();
        InterceptorChain mockInterceptorChain = mock(InterceptorChain.class);
        GenericEventMessage<String> eventMessage = new GenericEventMessage<>("payload");
        UnitOfWork<EventMessage<?>> unitOfWork = new DefaultUnitOfWork<>(eventMessage);
        unitOfWork.start();

        foodStatsRecorder.handle(unitOfWork, mockInterceptorChain);
        unitOfWork.commit();

        verify(mockInterceptorChain).proceed();
        double recordedRecipeAmount = meterRegistry.get(FOOD_ENTITY_METRIC_NAME).tag(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_RECIPES).gauge().value();
        assertThat(recordedRecipeAmount).isEqualTo(10);
        double recordedIngredientAmount = meterRegistry.get(FOOD_ENTITY_METRIC_NAME).tag(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_INGREDIENTS).gauge().value();
        assertThat(recordedIngredientAmount).isEqualTo(20);
        double recordedPropertyAmount = meterRegistry.get(FOOD_ENTITY_METRIC_NAME).tag(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_PROPERTIES).gauge().value();
        assertThat(recordedPropertyAmount).isEqualTo(5);
    }

    private static Stream<Arguments> should_record_updated_food_entity_count_when_intercepting_a_domain_event() {
        return Stream.of(
                Arguments.of(new RecipeCreatedEvent("id", "name"), 11, 20, 5),
                Arguments.of(new RecipeDeletedEvent("id", "name"), 9, 20, 5),
                Arguments.of(new IngredientCreatedEvent("id", "name"), 10, 21, 5),
                Arguments.of(new IngredientDeletedEvent("id", "name"), 10, 19, 5),
                Arguments.of(new PropertyCreatedEvent("id", "name"), 10, 20, 6),
                Arguments.of(new PropertyDeletedEvent("id", "name"), 10, 20, 4),
                Arguments.of(new RecipeIngredientAddedEvent("id", new RecipeIngredient("id", fixedTime())), 10, 20, 5)
        );
    }
}
