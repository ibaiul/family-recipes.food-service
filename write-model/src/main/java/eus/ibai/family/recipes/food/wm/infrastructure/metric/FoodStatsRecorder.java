package eus.ibai.family.recipes.food.wm.infrastructure.metric;

import eus.ibai.family.recipes.food.event.DomainEvent;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.IngredientNameConstraintRepository;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.PropertyNameConstraintRepository;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.RecipeNameConstraintRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class FoodStatsRecorder implements MessageHandlerInterceptor<EventMessage<?>> {

    static final String FOOD_ENTITY_METRIC_NAME = "food.entity";

    static final String FOOD_ENTITY_TAG_NAME = "name";

    static final String FOOD_ENTITY_TAG_RECIPES = "recipes";

    static final String FOOD_ENTITY_TAG_INGREDIENTS = "ingredients";

    static final String FOOD_ENTITY_TAG_PROPERTIES = "properties";

    private static final Map<String, AtomicLong> statsMap = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;

    private final RecipeNameConstraintRepository recipeRepository;

    private final IngredientNameConstraintRepository ingredientRepository;

    private final PropertyNameConstraintRepository propertyRepository;

    @PostConstruct
    public void recordInitialStats() {
        long recipeAmount = recipeRepository.count();
        AtomicLong recipeCount = meterRegistry.gauge(FOOD_ENTITY_METRIC_NAME, List.of(Tag.of(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_RECIPES)), new AtomicLong(recipeAmount), AtomicLong::get);
        statsMap.put(FOOD_ENTITY_TAG_RECIPES, recipeCount);
        long ingredientAmount = ingredientRepository.count();
        AtomicLong ingredientCount = meterRegistry.gauge(FOOD_ENTITY_METRIC_NAME, List.of(Tag.of(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_INGREDIENTS)), new AtomicLong(ingredientAmount), AtomicLong::get);
        statsMap.put(FOOD_ENTITY_TAG_INGREDIENTS, ingredientCount);
        long propertyAmount = propertyRepository.count();
        AtomicLong propertyCount = meterRegistry.gauge(FOOD_ENTITY_METRIC_NAME, List.of(Tag.of(FOOD_ENTITY_TAG_NAME, FOOD_ENTITY_TAG_PROPERTIES)), new AtomicLong(propertyAmount), AtomicLong::get);
        statsMap.put(FOOD_ENTITY_TAG_PROPERTIES, propertyCount);
    }

    @Override
    public Object handle(@Nonnull UnitOfWork<? extends EventMessage<?>> unitOfWork, @Nonnull InterceptorChain interceptorChain) throws Exception {
        Object eventMessage = unitOfWork.getMessage().getPayload();
        log.trace("Intercepting event {}", eventMessage);

        if (eventMessage instanceof DomainEvent<?> event) {
            unitOfWork.afterCommit(u -> {
                log.trace("Processing intercepted event {}", event);
                switch (event.type()) {
                    case "RecipeCreatedEvent" -> statsMap.get(FOOD_ENTITY_TAG_RECIPES).incrementAndGet();
                    case "RecipeDeletedEvent" -> statsMap.get(FOOD_ENTITY_TAG_RECIPES).decrementAndGet();
                    case "IngredientCreatedEvent" -> statsMap.get(FOOD_ENTITY_TAG_INGREDIENTS).incrementAndGet();
                    case "IngredientDeletedEvent" -> statsMap.get(FOOD_ENTITY_TAG_INGREDIENTS).decrementAndGet();
                    case "PropertyCreatedEvent" -> statsMap.get(FOOD_ENTITY_TAG_PROPERTIES).incrementAndGet();
                    case "PropertyDeletedEvent" -> statsMap.get(FOOD_ENTITY_TAG_PROPERTIES).decrementAndGet();
                    default -> log.trace("Ignored event type {} as it does not affect food stats", event.type());
                }
            });
        }

        return interceptorChain.proceed();
    }
}
