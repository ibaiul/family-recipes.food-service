package eus.ibai.family.recipes.food.rm.domain.property;

import eus.ibai.family.recipes.food.event.DomainEvent;
import eus.ibai.family.recipes.food.event.PropertyCreatedEvent;
import eus.ibai.family.recipes.food.event.PropertyDeletedEvent;
import eus.ibai.family.recipes.food.event.PropertyUpdatedEvent;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.PropertyEntityRepository;
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
class PropertyProjectionEventHandler {

    private final PropertyEntityRepository propertyEntityRepository;

    @EventHandler
    void on(PropertyCreatedEvent event) {
        logEvent(event);
        propertyEntityRepository.saveNew(event.aggregateId(), event.propertyName())
                .subscribe();
    }

    @EventHandler
    void on(PropertyUpdatedEvent event) {
        logEvent(event);
        propertyEntityRepository.findById(event.aggregateId())
                .flatMap(propertyEntity -> {
                    propertyEntity.setName(event.propertyName());
                    return propertyEntityRepository.save(propertyEntity);
                })
                .subscribe();
    }

    @EventHandler
    void on(PropertyDeletedEvent event) {
        logEvent(event);
        propertyEntityRepository.deleteById(event.aggregateId())
                .subscribe();
    }

    private void logEvent(DomainEvent<String> event) {
        log.debug("Updating property projection with event: {}", event);
    }
}
