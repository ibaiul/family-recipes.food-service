package eus.ibai.family.recipes.food.wm.domain.property;

import eus.ibai.family.recipes.food.event.PropertyCreatedEvent;
import eus.ibai.family.recipes.food.event.PropertyDeletedEvent;
import eus.ibai.family.recipes.food.event.PropertyUpdatedEvent;
import lombok.Getter;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;
import static org.axonframework.modelling.command.AggregateLifecycle.markDeleted;

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
        apply(new PropertyCreatedEvent(command.aggregateId(), command.propertyName()));
    }

    @EventSourcingHandler
    public void on(PropertyCreatedEvent event) {
        this.id = event.aggregateId();
        this.name = event.propertyName();
    }

    @CommandHandler
    public void handle(UpdatePropertyCommand command) {
        if (name.equals(command.propertyName())) {
            return;
        }
        apply(new PropertyUpdatedEvent(id, command.propertyName()));
    }

    @EventSourcingHandler
    public void on(PropertyUpdatedEvent event) {
        this.name = event.propertyName();
    }

    @CommandHandler
    public void handle(DeletePropertyCommand command) {
        apply(new PropertyDeletedEvent(id, name));
    }

    @EventSourcingHandler
    public void on(PropertyDeletedEvent event) {
        markDeleted();
    }
}
