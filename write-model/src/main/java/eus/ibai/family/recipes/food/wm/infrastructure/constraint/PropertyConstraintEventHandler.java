package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import eus.ibai.family.recipes.food.event.DomainEvent;
import eus.ibai.family.recipes.food.event.PropertyCreatedEvent;
import eus.ibai.family.recipes.food.event.PropertyDeletedEvent;
import eus.ibai.family.recipes.food.event.PropertyUpdatedEvent;
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
public class PropertyConstraintEventHandler {

    private final PropertyNameConstraintRepository propertyRepository;

    @EventHandler
    public void on(PropertyCreatedEvent event) {
        logEvent(event);
        propertyRepository.save(new PropertyNameConstraintEntity(event.aggregateId(), event.propertyName()));
    }

    @EventHandler
    public void on(PropertyUpdatedEvent event) {
        logEvent(event);
        PropertyNameConstraintEntity propertyEntity = propertyRepository.findById(event.aggregateId())
                .orElseThrow(() -> new ConstraintException("Failed to update property name constraint. Not found for event: " + event));
        propertyEntity.setPropertyName(event.propertyName());
        propertyRepository.save(propertyEntity);
    }

    @EventHandler
    public void on(PropertyDeletedEvent event) {
        logEvent(event);
        propertyRepository.deleteById(event.aggregateId());
    }

    private void logEvent(DomainEvent<String> event) {
        log.debug("Updating property constraints with event: {}", event);
    }
}
