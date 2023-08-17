package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import eus.ibai.family.recipes.food.event.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientConstraintEventHandlerTest {

    @Mock
    private IngredientNameConstraintRepository ingredientRepository;

    @Mock
    private IngredientPropertyConstraintRepository ingredientPropertyRepository;

    @InjectMocks
    private IngredientConstraintEventHandler eventHandler;

    @Test
    void should_create_ingredient_name_constraint() {
        IngredientCreatedEvent event = new IngredientCreatedEvent("ingredientId", "ingredientName");

        eventHandler.on(event);

        verify(ingredientRepository).save(new IngredientNameConstraintEntity(event.aggregateId(), event.ingredientName()));
    }

    @Test
    void should_update_ingredient_name_constraint() {
        IngredientNameConstraintEntity initialEntity = new IngredientNameConstraintEntity("ingredientId", "ingredientName");
        IngredientUpdatedEvent event = new IngredientUpdatedEvent(initialEntity.getIngredientId(), "newIngredientName");
        when(ingredientRepository.findById(event.aggregateId())).thenReturn(Optional.of(initialEntity));

        eventHandler.on(event);

        verify(ingredientRepository).save(new IngredientNameConstraintEntity(event.aggregateId(), event.ingredientName()));
    }

    @Test
    void should_delete_ingredient_name_constraint() {
        IngredientDeletedEvent event = new IngredientDeletedEvent("ingredientId", "ingredientName");

        eventHandler.on(event);

        verify(ingredientRepository).deleteById(event.aggregateId());
        verify(ingredientPropertyRepository).deleteByIngredientId(event.aggregateId());
    }

    @Test
    void should_add_ingredient_property_constraint() {
        IngredientPropertyAddedEvent event = new IngredientPropertyAddedEvent("ingredientId", new IngredientProperty("propertyId", LocalDateTime.now()));

        eventHandler.on(event);

        verify(ingredientPropertyRepository).save(new IngredientPropertyConstraintEntity(event.aggregateId(), event.ingredientProperty().propertyId()));
    }

    @Test
    void should_delete_ingredient_property_constraint() {
        IngredientPropertyRemovedEvent event = new IngredientPropertyRemovedEvent("ingredientId", new IngredientProperty("propertyId", LocalDateTime.now()));

        eventHandler.on(event);

        verify(ingredientPropertyRepository).deleteById(new IngredientPropertyId(event.aggregateId(), event.ingredientProperty().propertyId()));
    }
}