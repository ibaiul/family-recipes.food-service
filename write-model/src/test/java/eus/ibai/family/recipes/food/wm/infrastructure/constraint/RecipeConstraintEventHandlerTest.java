package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import eus.ibai.family.recipes.food.event.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeConstraintEventHandlerTest {

    @Mock
    private RecipeNameConstraintRepository recipeRepository;

    @Mock
    private RecipeIngredientConstraintRepository recipeIngredientRepository;

    @InjectMocks
    private RecipeConstraintEventHandler eventHandler;

    @Test
    void should_create_recipe_name_constraint() {
        RecipeCreatedEvent event = new RecipeCreatedEvent("recipeId", "recipeName");

        eventHandler.on(event);

        verify(recipeRepository).save(new RecipeNameConstraintEntity(event.aggregateId(), event.recipeName()));
    }

    @Test
    void should_update_recipe_name_constraint() {
        RecipeNameConstraintEntity initialEntity = new RecipeNameConstraintEntity("recipeId", "recipeName");
        RecipeUpdatedEvent event = new RecipeUpdatedEvent(initialEntity.getRecipeId(), "newRecipeName", Set.of("link"));
        when(recipeRepository.findById(event.aggregateId())).thenReturn(Optional.of(initialEntity));

        eventHandler.on(event);

        verify(recipeRepository).save(new RecipeNameConstraintEntity(event.aggregateId(), event.recipeName()));
    }

    @Test
    void should_delete_recipe_name_constraint() {
        RecipeDeletedEvent event = new RecipeDeletedEvent("recipeId", "recipeName");

        eventHandler.on(event);

        verify(recipeRepository).deleteById(event.aggregateId());
        verify(recipeIngredientRepository).deleteByRecipeId(event.aggregateId());
    }

    @Test
    void should_add_recipe_ingredient_constraint() {
        RecipeIngredientAddedEvent event = new RecipeIngredientAddedEvent("recipeId", new RecipeIngredient("ingredientId", LocalDateTime.now()));

        eventHandler.on(event);

        verify(recipeIngredientRepository).save(new RecipeIngredientConstraintEntity(event.aggregateId(), event.recipeIngredient().ingredientId()));
    }

    @Test
    void should_delete_recipe_ingredient_constraint() {
        RecipeIngredientRemovedEvent event = new RecipeIngredientRemovedEvent("recipeId", new RecipeIngredient("ingredientId", LocalDateTime.now()));

        eventHandler.on(event);

        verify(recipeIngredientRepository).deleteById(new RecipeIngredientId(event.aggregateId(), event.recipeIngredient().ingredientId()));
    }
}