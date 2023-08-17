package eus.ibai.family.recipes.food.rm.domain.ingredient;

import eus.ibai.family.recipes.food.event.*;
import eus.ibai.family.recipes.food.rm.infrastructure.model.IngredientEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.IngredientEntityRepository;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.IngredientPropertyEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static eus.ibai.family.recipes.food.test.TestUtils.fixedTime;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientProjectionEventHandlerTest {

    @Mock
    private IngredientEntityRepository ingredientRepository;

    @Mock
    private IngredientPropertyEntityRepository ingredientPropertyRepository;

    @InjectMocks
    private IngredientProjectionEventHandler ingredientProjectionEventHandler;

    @Test
    void should_persist_new_ingredient_when_ingredient_created_event_received() {
        IngredientCreatedEvent event = new IngredientCreatedEvent(generateId(), "Spaghetti");
        when(ingredientRepository.saveNew(event.aggregateId(), event.ingredientName())).thenReturn(Mono.empty());

        ingredientProjectionEventHandler.on(event);

        verify(ingredientRepository).saveNew(event.aggregateId(), event.ingredientName());
    }

    @Test
    void should_update_ingredient_when_ingredient_updated_event_received() {
        IngredientUpdatedEvent event = new IngredientUpdatedEvent(generateId(), "Spaghetti integral");
        IngredientEntity initialEntity = new IngredientEntity(event.aggregateId(), "Spaghetti");
        IngredientEntity expectedEntity = new IngredientEntity(event.aggregateId(), event.ingredientName());
        when(ingredientRepository.findById(event.aggregateId())).thenReturn(Mono.just(initialEntity));
        when(ingredientRepository.save(expectedEntity)).thenReturn(Mono.empty());

        ingredientProjectionEventHandler.on(event);

        verify(ingredientRepository).save(expectedEntity);
    }


    @Test
    void should_delete_ingredient_when_ingredient_deleted_event_received() {
        IngredientDeletedEvent event = new IngredientDeletedEvent(generateId(), "Spaghetti");
        when(ingredientPropertyRepository.deleteByIngredientId(event.aggregateId())).thenReturn(Mono.empty());
        when(ingredientRepository.deleteById(event.aggregateId())).thenReturn(Mono.empty());

        ingredientProjectionEventHandler.on(event);

        verify(ingredientRepository).deleteById(event.aggregateId());
    }


    @Test
    void should_add_ingredient_property_when_ingredient_property_added_event_received() {
        IngredientPropertyAddedEvent event = new IngredientPropertyAddedEvent(generateId(), new IngredientProperty(generateId(), fixedTime()));
        when(ingredientPropertyRepository.saveNew(event.aggregateId(), event.ingredientProperty().propertyId(), fixedTime())).thenReturn(Mono.empty());

        ingredientProjectionEventHandler.on(event);

        verify(ingredientPropertyRepository).saveNew(event.aggregateId(), event.ingredientProperty().propertyId(), fixedTime());
    }


    @Test
    void should_remove_ingredient_property_when_ingredient_property_removed_event_received() {
        IngredientPropertyRemovedEvent event = new IngredientPropertyRemovedEvent(generateId(), new IngredientProperty(generateId(), fixedTime()));
        when(ingredientPropertyRepository.deleteByIngredientIdAndPropertyId(event.aggregateId(), event.ingredientProperty().propertyId())).thenReturn(Mono.empty());

        ingredientProjectionEventHandler.on(event);

        verify(ingredientPropertyRepository).deleteByIngredientIdAndPropertyId(event.aggregateId(), event.ingredientProperty().propertyId());
    }
}