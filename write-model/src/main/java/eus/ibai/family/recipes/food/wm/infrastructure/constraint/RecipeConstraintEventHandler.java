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
public class RecipeConstraintEventHandler {

    private final RecipeNameConstraintRepository recipeRepository;

    private final RecipeIngredientConstraintRepository recipeIngredientRepository;

    @EventHandler
    public void on(RecipeCreatedEvent event) {
        logEvent(event);
        recipeRepository.save(new RecipeNameConstraintEntity(event.aggregateId(), event.recipeName()));
    }

    @EventHandler
    public void on(RecipeUpdatedEvent event) {
        logEvent(event);
        RecipeNameConstraintEntity recipeEntity = recipeRepository.findById(event.aggregateId())
                .orElseThrow(() -> new ConstraintException("Failed to update recipe name constraint. Not found for event: " + event));
        recipeEntity.setRecipeName(event.recipeName());
        recipeRepository.save(recipeEntity);
    }

    @EventHandler
    public void on(RecipeDeletedEvent event) {
        logEvent(event);
        recipeIngredientRepository.deleteByRecipeId(event.aggregateId());
        recipeRepository.deleteById(event.aggregateId());
    }

    @EventHandler
    public void on(RecipeIngredientAddedEvent event) {
        logEvent(event);
        recipeIngredientRepository.save(new RecipeIngredientConstraintEntity(event.aggregateId(), event.recipeIngredient().ingredientId()));
    }

    @EventHandler
    public void on(RecipeIngredientRemovedEvent event) {
        logEvent(event);
        recipeIngredientRepository.deleteById(new RecipeIngredientId(event.aggregateId(), event.recipeIngredient().ingredientId()));
    }

    private void logEvent(DomainEvent<String> event) {
        log.debug("Updating recipe constraints with event: {}", event);
    }
}
