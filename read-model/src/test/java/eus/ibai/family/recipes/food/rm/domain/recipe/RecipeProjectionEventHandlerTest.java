package eus.ibai.family.recipes.food.rm.domain.recipe;

import eus.ibai.family.recipes.food.event.*;
import eus.ibai.family.recipes.food.rm.infrastructure.model.RecipeEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.RecipeEntityRepository;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.RecipeIngredientEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Set;

import static eus.ibai.family.recipes.food.test.TestUtils.fixedTime;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeProjectionEventHandlerTest {

    @Mock
    private RecipeEntityRepository recipeRepository;

    @Mock
    private RecipeIngredientEntityRepository recipeIngredientRepository;

    @InjectMocks
    private RecipeProjectionEventHandler recipeProjectionEventHandler;

    @Test
    void should_persist_new_recipe_when_recipe_created_event_received() {
        RecipeCreatedEvent event = new RecipeCreatedEvent(generateId(), "Pasta carbonara");
        when(recipeRepository.saveNew(event.aggregateId(), event.recipeName())).thenReturn(Mono.empty());

        recipeProjectionEventHandler.on(event);

        verify(recipeRepository).saveNew(event.aggregateId(), event.recipeName());
    }

    @Test
    void should_update_recipe_when_recipe_updated_event_received() {
        RecipeUpdatedEvent event = new RecipeUpdatedEvent(generateId(), "Spaghetti carbonara", Set.of("https://pasta.com"));
        RecipeEntity initialEntity = new RecipeEntity(event.aggregateId(), "Pasta carbonara");
        RecipeEntity expectedEntity = new RecipeEntity(event.aggregateId(), event.recipeName(), Set.of("https://pasta.com"));
        when(recipeRepository.findById(event.aggregateId())).thenReturn(Mono.just(initialEntity));
        when(recipeRepository.save(expectedEntity)).thenReturn(Mono.empty());

        recipeProjectionEventHandler.on(event);

        verify(recipeRepository).save(expectedEntity);
    }

    @Test
    void should_delete_recipe_when_recipe_deleted_event_received() {
        RecipeDeletedEvent event = new RecipeDeletedEvent(generateId(), "Pasta carbonara");
        when(recipeIngredientRepository.deleteByRecipeId(event.aggregateId())).thenReturn(Mono.empty());
        when(recipeRepository.deleteById(event.aggregateId())).thenReturn(Mono.empty());

        recipeProjectionEventHandler.on(event);

        verify(recipeRepository).deleteById(event.aggregateId());
    }

    @Test
    void should_add_recipe_ingredient_when_recipe_ingredient_added_event_received() {
        RecipeIngredientAddedEvent event = new RecipeIngredientAddedEvent(generateId(), new RecipeIngredient(generateId(), fixedTime()));
        when(recipeIngredientRepository.saveNew(event.aggregateId(), event.recipeIngredient().ingredientId(), fixedTime())).thenReturn(Mono.empty());

        recipeProjectionEventHandler.on(event);

        verify(recipeIngredientRepository).saveNew(event.aggregateId(), event.recipeIngredient().ingredientId(), fixedTime());
    }

    @Test
    void should_remove_recipe_ingredient_when_recipe_ingredient_removed_event_received() {
        RecipeIngredientRemovedEvent event = new RecipeIngredientRemovedEvent(generateId(), new RecipeIngredient(generateId(), fixedTime()));
        when(recipeIngredientRepository.deleteByRecipeIdAndIngredientId(event.aggregateId(), event.recipeIngredient().ingredientId())).thenReturn(Mono.empty());

        recipeProjectionEventHandler.on(event);

        verify(recipeIngredientRepository).deleteByRecipeIdAndIngredientId(event.aggregateId(), event.recipeIngredient().ingredientId());
    }

    @Test
    void should_add_recipe_tag_when_recipe_tag_added_event_received() {
        RecipeTagAddedEvent event = new RecipeTagAddedEvent(generateId(), "First course");
        RecipeEntity initialEntity = new RecipeEntity(event.aggregateId(), "Pasta carbonara");
        RecipeEntity expectedEntity = new RecipeEntity(event.aggregateId(), initialEntity.getName(), initialEntity.getLinks())
                .addTag(event.recipeTag());
        when(recipeRepository.findById(event.aggregateId())).thenReturn(Mono.just(initialEntity));

        recipeProjectionEventHandler.on(event);

        verify(recipeRepository).save(expectedEntity);
    }

    @Test
    void should_remove_recipe_tag_when_recipe_tag_removed_event_received() {
        RecipeTagRemovedEvent event = new RecipeTagRemovedEvent(generateId(), "First course");
        RecipeEntity initialEntity = new RecipeEntity(event.aggregateId(), "Pasta carbonara")
                .addTag("Pasta")
                .addTag(event.recipeTag());
        RecipeEntity expectedEntity = new RecipeEntity(event.aggregateId(), initialEntity.getName(), initialEntity.getLinks())
                .addTag("Pasta");
        when(recipeRepository.findById(event.aggregateId())).thenReturn(Mono.just(initialEntity));

        recipeProjectionEventHandler.on(event);

        verify(recipeRepository).save(expectedEntity);
    }

    @Test
    void should_add_recipe_image_when_recipe_image_added_event_received() {
        RecipeImageAddedEvent event = new RecipeImageAddedEvent(generateId(), "imageId");
        RecipeEntity initialEntity = new RecipeEntity(event.aggregateId(), "Pasta carbonara");
        RecipeEntity expectedEntity = new RecipeEntity(event.aggregateId(), initialEntity.getName(), initialEntity.getLinks())
                .addImage(event.imageId());
        when(recipeRepository.findById(event.aggregateId())).thenReturn(Mono.just(initialEntity));

        recipeProjectionEventHandler.on(event);

        verify(recipeRepository).save(expectedEntity);
    }

    @Test
    void should_remove_recipe_image_when_recipe_image_removed_event_received() {
        RecipeImageRemovedEvent event = new RecipeImageRemovedEvent(generateId(), "imageId");
        RecipeEntity initialEntity = new RecipeEntity(event.aggregateId(), "Pasta carbonara")
                .addImage(event.imageId())
                .addImage("imageId2");
        RecipeEntity expectedEntity = new RecipeEntity(event.aggregateId(), initialEntity.getName())
                .addImage("imageId2");
        when(recipeRepository.findById(event.aggregateId())).thenReturn(Mono.just(initialEntity));

        recipeProjectionEventHandler.on(event);

        verify(recipeRepository).save(expectedEntity);
    }
}