package eus.ibai.family.recipes.food.wm.domain.property;

import eus.ibai.family.recipes.food.event.PropertyCreatedEvent;
import eus.ibai.family.recipes.food.event.PropertyDeletedEvent;
import eus.ibai.family.recipes.food.event.PropertyUpdatedEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

@Slf4j
@Getter
@Aggregate
public class PropertyAggregate {

    @AggregateIdentifier
    private String id;

    private String name;

    protected PropertyAggregate() {
    }

    @CommandHandler
    public PropertyAggregate(CreatePropertyCommand command) {
        log.debug("Processing command: {}", command);
        AggregateLifecycle.apply(new PropertyCreatedEvent(command.aggregateId(), command.propertyName()));
    }

    @EventSourcingHandler
    public void on(PropertyCreatedEvent event) {
        log.debug("Applying event: {}", event);
        this.id = event.aggregateId();
        this.name = event.propertyName();
        log.debug("PropertyAggregate created: {}, {}", id, name);
    }

    @CommandHandler
    public void handle(UpdatePropertyCommand command) {
        if (name.equals(command.propertyName())) {
            return;
        }
        AggregateLifecycle.apply(new PropertyUpdatedEvent(id, command.propertyName()));
    }

    @EventSourcingHandler
    public void on(PropertyUpdatedEvent event) {
        this.name = event.propertyName();
    }

    @CommandHandler
    public void handle(DeletePropertyCommand command) {
        AggregateLifecycle.apply(new PropertyDeletedEvent(id, name));
    }

    @EventSourcingHandler
    public void on(PropertyDeletedEvent event) {
        AggregateLifecycle.markDeleted();
    }
}
