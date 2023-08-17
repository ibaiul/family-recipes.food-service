package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import eus.ibai.family.recipes.food.event.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Service;

import static eus.ibai.family.recipes.food.wm.infrastructure.config.AxonConfig.CONSTRAINT_EVENT_PROCESSOR;

@Slf4j
@Service
@AllArgsConstructor
@ProcessingGroup(CONSTRAINT_EVENT_PROCESSOR)
public class IngredientConstraintEventHandler {

    private final IngredientNameConstraintRepository ingredientRepository;

    private final IngredientPropertyConstraintRepository ingredientPropertyRepository;
    
    @EventHandler
    public void on(IngredientCreatedEvent event) {
        logEvent(event);
        ingredientRepository.save(new IngredientNameConstraintEntity(event.aggregateId(), event.ingredientName()));
    }

    @EventHandler
    public void on(IngredientUpdatedEvent event) {
        logEvent(event);
        IngredientNameConstraintEntity ingredientEntity = ingredientRepository.findById(event.aggregateId())
                .orElseThrow(() -> new ConstraintException("Failed to update ingredient name constraint. Not found for event: " + event));
        ingredientEntity.setIngredientName(event.ingredientName());
        ingredientRepository.save(ingredientEntity);
    }

    @EventHandler
    public void on(IngredientDeletedEvent event) {
        logEvent(event);
        ingredientPropertyRepository.deleteByIngredientId(event.aggregateId());
        ingredientRepository.deleteById(event.aggregateId());
    }

    @EventHandler
    public void on(IngredientPropertyAddedEvent event) {
        logEvent(event);
        ingredientPropertyRepository.save(new IngredientPropertyConstraintEntity(event.aggregateId(), event.ingredientProperty().propertyId()));
    }

    @EventHandler
    public void on(IngredientPropertyRemovedEvent event) {
        logEvent(event);
        ingredientPropertyRepository.deleteById(new IngredientPropertyId(event.aggregateId(), event.ingredientProperty().propertyId()));
    }

    private void logEvent(DomainEvent<String> event) {
        log.debug("Updating ingredient constraints with event: {}", event);
    }
}
