package eus.ibai.family.recipes.food.wm.domain.ingredient;

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
public class IngredientAggregate {

    @AggregateIdentifier
    private String id;

    private String name;

    @AggregateMember
    private final Set<IngredientPropertyEntity> properties = new HashSet<>();

    protected IngredientAggregate() {
    }

    @CommandHandler
    public IngredientAggregate(CreateIngredientCommand command) {
        AggregateLifecycle.apply(new IngredientCreatedEvent(command.aggregateId(), command.ingredientName()));
    }

    @EventSourcingHandler
    public void on(IngredientCreatedEvent event) {
        this.id = event.aggregateId();
        this.name = event.ingredientName();
    }

    @CommandHandler
    public void handle(UpdateIngredientCommand command) {
        if (name.equals(command.ingredientName())) {
            return;
        }
        AggregateLifecycle.apply(new IngredientUpdatedEvent(id, command.ingredientName()));
    }

    @EventSourcingHandler
    public void on(IngredientUpdatedEvent event) {
        this.name = event.ingredientName();
    }

    @CommandHandler
    public void handle(AddIngredientPropertyCommand command, Clock clock) {
        boolean containsIngredientProperty = properties.stream().anyMatch(ingredientProperty -> ingredientProperty.getPropertyId().equals(command.propertyId()));
        if (containsIngredientProperty) {
            throw new IngredientPropertyAlreadyAddedException("Ingredient: " + id + ", Property: " + command.propertyId());
        }

        IngredientProperty addedIngredientProperty = new IngredientProperty(command.propertyId(), now(clock));
        AggregateLifecycle.apply(new IngredientPropertyAddedEvent(id, addedIngredientProperty));
    }

    @EventSourcingHandler
    public void on(IngredientPropertyAddedEvent event) {
        IngredientPropertyEntity ingredientProperty = new IngredientPropertyEntity(event.ingredientProperty().propertyId(), event.ingredientProperty().addedOn());
        properties.add(ingredientProperty);
    }

    @CommandHandler
    public void handle(RemoveIngredientPropertyCommand command) {
        IngredientPropertyEntity removedIngredientProperty = properties.stream().filter(ingredientProperty -> ingredientProperty.getPropertyId().equals(command.propertyId())).findFirst()
                .orElseThrow(() -> new IngredientPropertyNotFoundException("Ingredient: " + this + ", Property: " + command.propertyId()));
        AggregateLifecycle.apply(new IngredientPropertyRemovedEvent(id, new IngredientProperty(removedIngredientProperty.getPropertyId(), removedIngredientProperty.getAddedOn())));
    }

    @EventSourcingHandler
    public void on(IngredientPropertyRemovedEvent event) {
        IngredientPropertyEntity removedIngredientProperty = new IngredientPropertyEntity(event.ingredientProperty().propertyId(), event.ingredientProperty().addedOn());
        properties.remove(removedIngredientProperty);
    }

    @CommandHandler
    public void handle(DeleteIngredientCommand command) {
        AggregateLifecycle.apply(new IngredientDeletedEvent(id, name));
    }

    @EventSourcingHandler
    public void on(IngredientDeletedEvent event) {
        AggregateLifecycle.markDeleted();
    }
}
