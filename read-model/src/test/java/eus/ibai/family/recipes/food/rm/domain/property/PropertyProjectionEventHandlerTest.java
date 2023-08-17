package eus.ibai.family.recipes.food.rm.domain.property;

import eus.ibai.family.recipes.food.event.PropertyCreatedEvent;
import eus.ibai.family.recipes.food.event.PropertyDeletedEvent;
import eus.ibai.family.recipes.food.event.PropertyUpdatedEvent;
import eus.ibai.family.recipes.food.rm.infrastructure.model.PropertyEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.PropertyEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyProjectionEventHandlerTest {

    @Mock
    private PropertyEntityRepository propertyEntityRepository;

    @InjectMocks
    private PropertyProjectionEventHandler propertyProjectionEventHandler;

    @Test
    void should_persist_new_property_when_property_created_event_received() {
        PropertyCreatedEvent event = new PropertyCreatedEvent(generateId(), "Vitamin C");
        when(propertyEntityRepository.saveNew(event.aggregateId(), event.propertyName())).thenReturn(Mono.empty());

        propertyProjectionEventHandler.on(event);

        verify(propertyEntityRepository).saveNew(event.aggregateId(), event.propertyName());
    }

    @Test
    void should_update_property_when_property_updated_event_received() {
        PropertyUpdatedEvent event = new PropertyUpdatedEvent(generateId(), "Vitamin-C");
        PropertyEntity initialEntity = new PropertyEntity(event.aggregateId(), "Vitamin C");
        PropertyEntity expectedEntity = new PropertyEntity(event.aggregateId(), event.propertyName());
        when(propertyEntityRepository.findById(event.aggregateId())).thenReturn(Mono.just(initialEntity));
        when(propertyEntityRepository.save(expectedEntity)).thenReturn(Mono.empty());

        propertyProjectionEventHandler.on(event);

        verify(propertyEntityRepository).save(expectedEntity);
    }


    @Test
    void should_delete_property_when_property_deleted_event_received() {
        PropertyDeletedEvent event = new PropertyDeletedEvent(generateId(), "Vitamin-C");
        when(propertyEntityRepository.deleteById(event.aggregateId())).thenReturn(Mono.empty());

        propertyProjectionEventHandler.on(event);

        verify(propertyEntityRepository).deleteById(event.aggregateId());
    }
}