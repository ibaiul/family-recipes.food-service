package eus.ibai.family.recipes.food.rm.domain.ingredient;

import eus.ibai.family.recipes.food.event.*;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.IngredientEntityRepository;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.IngredientPropertyEntityRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

import static eus.ibai.family.recipes.food.rm.infrastructure.config.AxonConfig.PROJECTIONS_EVENT_PROCESSOR;

@Slf4j
@Service
@AllArgsConstructor
@ProcessingGroup(PROJECTIONS_EVENT_PROCESSOR)
class IngredientProjectionEventHandler {

    private final IngredientEntityRepository ingredientEntityRepository;

    private final IngredientPropertyEntityRepository ingredientPropertyEntityRepository;

    @EventHandler
    void on(IngredientCreatedEvent event) {
        logEvent(event);
        ingredientEntityRepository.saveNew(event.aggregateId(), event.ingredientName())
                .subscribe();
    }

    @EventHandler
    void on(IngredientUpdatedEvent event) {
        logEvent(event);
        ingredientEntityRepository.findById(event.aggregateId())
                .flatMap(ingredientEntity -> {
                    ingredientEntity.setName(event.ingredientName());
                    return ingredientEntityRepository.save(ingredientEntity);
                })
                .subscribe();
    }

    @EventHandler
    void on(IngredientDeletedEvent event) {
        logEvent(event);
        ingredientPropertyEntityRepository.deleteByIngredientId(event.aggregateId())
                        .then(ingredientEntityRepository.deleteById(event.aggregateId()))
                .subscribe();
    }

    @EventHandler
    void on(IngredientPropertyAddedEvent event) {
        logEvent(event);
        ingredientPropertyEntityRepository.saveNew(event.aggregateId(), event.ingredientProperty().propertyId(), event.ingredientProperty().addedOn())
                .subscribe();
    }
    @EventHandler
    void on(IngredientPropertyRemovedEvent event) {
        logEvent(event);
        ingredientPropertyEntityRepository.deleteByIngredientIdAndPropertyId(event.aggregateId(), event.ingredientProperty().propertyId())
                .subscribe();
    }

    private void logEvent(DomainEvent<String> event) {
        log.debug("Updating ingredient projection with event: {}", event);
    }
}
