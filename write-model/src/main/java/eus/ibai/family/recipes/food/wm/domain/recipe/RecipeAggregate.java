package eus.ibai.family.recipes.food.wm.domain.recipe;

import eus.ibai.family.recipes.food.event.*;
import lombok.Getter;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.AggregateMember;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.Clock;
import java.util.HashSet;
import java.util.Set;

import static java.time.LocalDateTime.now;

@Getter
@Aggregate
public class RecipeAggregate {

    @AggregateIdentifier
    private String id;

    private String name;

    private final Set<String> links = new HashSet<>();

    @AggregateMember
    private final Set<RecipeIngredientEntity> ingredients = new HashSet<>();

    protected RecipeAggregate() {
    }

    @CommandHandler
    public RecipeAggregate(CreateRecipeCommand command) {
        AggregateLifecycle.apply(new RecipeCreatedEvent(command.aggregateId(), command.recipeName()));
    }

    @EventSourcingHandler
    public void on(RecipeCreatedEvent event) {
        this.id = event.aggregateId();
        this.name = event.recipeName();
    }

    @CommandHandler
    public void handle(UpdateRecipeCommand command) {
        if (name.equals(command.recipeName()) && links.equals(command.recipeLinks())) {
            return;
        }
        AggregateLifecycle.apply(new RecipeUpdatedEvent(id, command.recipeName(), command.recipeLinks()));
    }

    @EventSourcingHandler
    public void on(RecipeUpdatedEvent event) {
        name = event.recipeName();
        links.clear();
        links.addAll(event.recipeLinks());
    }

    @CommandHandler
    public void handle(AddRecipeIngredientCommand command, Clock clock) {
        boolean containsRecipeIngredient = ingredients.stream().anyMatch(recipeIngredient -> recipeIngredient.getIngredientId().equals(command.ingredientId()));
        if (containsRecipeIngredient) {
            throw new RecipeIngredientAlreadyAddedException("Recipe: " + id + ", Ingredient: " + command.ingredientId());
        }

        RecipeIngredient addedRecipeIngredient = new RecipeIngredient(command.ingredientId(), now(clock));
        AggregateLifecycle.apply(new RecipeIngredientAddedEvent(id, addedRecipeIngredient));
    }

    @EventSourcingHandler
    public void on(RecipeIngredientAddedEvent event) {
        RecipeIngredientEntity recipeIngredient = new RecipeIngredientEntity(event.recipeIngredient().ingredientId(), event.recipeIngredient().addedOn());
        ingredients.add(recipeIngredient);
    }

    @CommandHandler
    public void handle(RemoveRecipeIngredientCommand command) {
        RecipeIngredientEntity removedRecipeIngredient = ingredients.stream().filter(recipeIngredient -> recipeIngredient.getIngredientId().equals(command.ingredientId())).findFirst()
                .orElseThrow(() -> new RecipeIngredientNotFoundException("Recipe: " + this + ", Ingredient: " + command.ingredientId()));
        AggregateLifecycle.apply(new RecipeIngredientRemovedEvent(id, new RecipeIngredient(removedRecipeIngredient.getIngredientId(), removedRecipeIngredient.getAddedOn())));
    }

    @EventSourcingHandler
    public void on(RecipeIngredientRemovedEvent event) {
        RecipeIngredientEntity removedRecipeIngredient = new RecipeIngredientEntity(event.recipeIngredient().ingredientId(), event.recipeIngredient().addedOn());
        ingredients.remove(removedRecipeIngredient);
    }

    @CommandHandler
    public void handle(DeleteRecipeCommand command) {
        AggregateLifecycle.apply(new RecipeDeletedEvent(id, name));
    }

    @EventSourcingHandler
    public void on(RecipeDeletedEvent event) {
        AggregateLifecycle.markDeleted();
    }
}
