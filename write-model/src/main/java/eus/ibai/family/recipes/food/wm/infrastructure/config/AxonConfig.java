package eus.ibai.family.recipes.food.wm.infrastructure.config;

import eus.ibai.family.recipes.food.wm.infrastructure.constraint.IngredientNameConstraintRepository;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.PropertyNameConstraintRepository;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.RecipeNameConstraintRepository;
import eus.ibai.family.recipes.food.wm.infrastructure.metric.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.PropagatingErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@RequiredArgsConstructor
public class AxonConfig {

    public static final String CONSTRAINT_EVENT_PROCESSOR = "constraint-event-processor";

    private final MeterRegistry meterRegistry;

    private final RecipeNameConstraintRepository recipeRepository;

    private final IngredientNameConstraintRepository ingredientRepository;

    private final PropertyNameConstraintRepository propertyRepository;

    @Autowired
    public void configureEventProcessing(EventProcessingConfigurer configurer) {
        FoodStatsRecorder foodStatsRecorder = new FoodStatsRecorder(meterRegistry, recipeRepository, ingredientRepository, propertyRepository);
        foodStatsRecorder.recordInitialStats();
        configurer
                .usingSubscribingEventProcessors()
                .registerListenerInvocationErrorHandler(CONSTRAINT_EVENT_PROCESSOR, conf -> PropagatingErrorHandler.instance())
                .registerHandlerInterceptor(CONSTRAINT_EVENT_PROCESSOR, configuration -> foodStatsRecorder)
                .registerHandlerInterceptor(CONSTRAINT_EVENT_PROCESSOR,configuration -> new EventProcessedInterceptor(meterRegistry));
    }

    @Autowired
    public void configureEventBus(EventBus eventBus) {
        eventBus.registerDispatchInterceptor(new EventDispatchedInterceptor(meterRegistry));
    }

    @Autowired
    public void configureCommandBus(CommandBus commandBus) {
        commandBus.registerDispatchInterceptor(new CommandDispatchedInterceptor(meterRegistry));
        commandBus.registerHandlerInterceptor(new CommandProcessedInterceptor(meterRegistry));
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
