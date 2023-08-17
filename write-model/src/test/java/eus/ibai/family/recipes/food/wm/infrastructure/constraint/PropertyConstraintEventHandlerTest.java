package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import eus.ibai.family.recipes.food.event.PropertyCreatedEvent;
import eus.ibai.family.recipes.food.event.PropertyDeletedEvent;
import eus.ibai.family.recipes.food.event.PropertyUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyConstraintEventHandlerTest {

    @Mock
    private PropertyNameConstraintRepository propertyRepository;

    @InjectMocks
    private PropertyConstraintEventHandler eventHandler;

    @Test
    void should_create_property_name_constraint() {
        PropertyCreatedEvent event = new PropertyCreatedEvent("propertyId", "propertyName");

        eventHandler.on(event);

        verify(propertyRepository).save(new PropertyNameConstraintEntity(event.aggregateId(), event.propertyName()));
    }

    @Test
    void should_update_property_name_constraint() {
        PropertyNameConstraintEntity initialEntity = new PropertyNameConstraintEntity("propertyId", "propertyName");
        PropertyUpdatedEvent event = new PropertyUpdatedEvent(initialEntity.getPropertyId(), "newPropertyName");
        when(propertyRepository.findById(event.aggregateId())).thenReturn(Optional.of(initialEntity));

        eventHandler.on(event);

        verify(propertyRepository).save(new PropertyNameConstraintEntity(event.aggregateId(), event.propertyName()));
    }

    @Test
    void should_not_update_property_name_constraint_if_does_not_exist() {
        PropertyUpdatedEvent event = new PropertyUpdatedEvent("propertyId", "newPropertyName");
        when(propertyRepository.findById(event.aggregateId())).thenReturn(Optional.empty());

        assertThrows(ConstraintException.class, () -> eventHandler.on(event));
    }

    @Test
    void should_delete_property_name_constraint() {
        PropertyDeletedEvent event = new PropertyDeletedEvent("propertyId", "propertyName");

        eventHandler.on(event);

        verify(propertyRepository).deleteById(event.aggregateId());
    }
}