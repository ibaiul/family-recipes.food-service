package eus.ibai.family.recipes.food.rm.domain.recipe;

import eus.ibai.family.recipes.food.event.*;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.RecipeEntityRepository;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.RecipeIngredientEntityRepository;
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
class RecipeProjectionEventHandler {

    private final RecipeEntityRepository recipeEntityRepository;

    private final RecipeIngredientEntityRepository recipeIngredientEntityRepository;

    @EventHandler
    void on(RecipeCreatedEvent event) {
        logEvent(event);
        recipeEntityRepository.saveNew(event.aggregateId(), event.recipeName())
                .subscribe();
    }

    @EventHandler
    void on(RecipeUpdatedEvent event) {
        logEvent(event);
        recipeEntityRepository.findById(event.aggregateId())
                .flatMap(recipeEntity -> {
                    recipeEntity.setName(event.recipeName());
                    recipeEntity.setLinks(event.recipeLinks());
                    return recipeEntityRepository.save(recipeEntity);
                })
                .subscribe();
    }

    @EventHandler
    void on(RecipeDeletedEvent event) {
        logEvent(event);
        recipeIngredientEntityRepository.deleteByRecipeId(event.aggregateId())
                        .then(recipeEntityRepository.deleteById(event.aggregateId()))
                .subscribe();
    }

    @EventHandler
    void on(RecipeIngredientAddedEvent event) {
        logEvent(event);
        recipeIngredientEntityRepository.saveNew(event.aggregateId(), event.recipeIngredient().ingredientId(), event.recipeIngredient().addedOn())
                .subscribe();
    }

    @EventHandler
    void on(RecipeIngredientRemovedEvent event) {
        logEvent(event);
        recipeIngredientEntityRepository.deleteByRecipeIdAndIngredientId(event.aggregateId(), event.recipeIngredient().ingredientId())
                .subscribe();
    }

    @EventHandler
    void on(RecipeTagAddedEvent event) {
        logEvent(event);
        recipeEntityRepository.findById(event.aggregateId())
                .map(recipeEntity -> recipeEntity.addTag(event.recipeTag()))
                .flatMap(recipeEntityRepository::save)
                .subscribe();
    }

    @EventHandler
    void on(RecipeTagRemovedEvent event) {
        logEvent(event);
        recipeEntityRepository.findById(event.aggregateId())
                .map(recipeEntity -> recipeEntity.removeTag(event.recipeTag()))
                .flatMap(recipeEntityRepository::save)
                .subscribe();
    }

    @EventHandler
    void on(RecipeImageAddedEvent event) {
        logEvent(event);
        recipeEntityRepository.findById(event.aggregateId())
                .map(recipeEntity -> recipeEntity.addImage(event.imageId()))
                .flatMap(recipeEntityRepository::save)
                .subscribe();
    }

    @EventHandler
    void on(RecipeImageRemovedEvent event) {
        logEvent(event);
        recipeEntityRepository.findById(event.aggregateId())
                .map(recipeEntity -> recipeEntity.removeImage(event.imageId()))
                .flatMap(recipeEntityRepository::save)
                .subscribe();
    }

    private void logEvent(DomainEvent<String> event) {
        log.debug("Updating recipe projection with event: {}", event);
    }
}
