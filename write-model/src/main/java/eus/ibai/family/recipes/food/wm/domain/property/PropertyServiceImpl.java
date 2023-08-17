package eus.ibai.family.recipes.food.wm.domain.property;

import eus.ibai.family.recipes.food.exception.PropertyNotFoundException;
import eus.ibai.family.recipes.food.wm.domain.command.AggregateCommand;
import eus.ibai.family.recipes.food.wm.infrastructure.exception.DownstreamConnectivityException;
import lombok.AllArgsConstructor;
import org.axonframework.eventsourcing.AggregateDeletedException;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static eus.ibai.family.recipes.food.util.Utils.generateId;

/**
 * Domain service to enforce invariants out of the scope of a single aggregate.
 */

@Service
@AllArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private final ReactorCommandGateway commandGateway;

    private final PropertyConstraintRepository propertyConstraintRepository;

    @Override
    public Mono<String> createProperty(String propertyName) {
        return Mono.defer(() -> Mono.just(propertyConstraintRepository.nameExists(propertyName)))
                .onErrorMap(t -> new DownstreamConnectivityException("Could not determine if property name exists.", t))
                .filter(exists -> exists)
                .handle((property, sink) -> sink.error(new PropertyAlreadyExistsException(propertyName)))
                .switchIfEmpty(Mono.defer( () -> Mono.just(new CreatePropertyCommand(generateId(), propertyName))))
                .flatMap(commandGateway::send);
    }

    @Override
    public Mono<Void> updateProperty(String propertyId, String propertyName) {
        return Mono.defer(() -> Mono.just(propertyConstraintRepository.anotherPropertyContainsName(propertyId, propertyName)))
                .onErrorMap(t -> new DownstreamConnectivityException("Could not determine if property name exists.", t))
                .filter(exists -> exists)
                .handle((property, sink) -> sink.error(new PropertyAlreadyExistsException(propertyName)))
                .switchIfEmpty(Mono.defer(() -> send(new UpdatePropertyCommand(propertyId, propertyName))))
                .then();
    }

    @Override
    public Mono<Void> deleteProperty(String propertyId) {
        return Mono.defer(() -> Mono.just(propertyConstraintRepository.isPropertyBoundToIngredients(propertyId)))
                .onErrorMap(t -> new DownstreamConnectivityException("Could not determine if property bound to any ingredient.", t))
                .filter(isBound -> !isBound)
                .switchIfEmpty(Mono.error(new PropertyAttachedToIngredientException("Property:" + propertyId)))
                .flatMap(isNotBound -> Mono.defer(() -> send(new DeletePropertyCommand(propertyId))));
    }

    private Mono<Void> send(AggregateCommand<String> command) {
        return commandGateway.send(command)
                .onErrorMap(AggregateNotFoundException.class, t -> new PropertyNotFoundException(command.aggregateId()))
                .onErrorMap(AggregateDeletedException.class, t -> new PropertyNotFoundException("Property is deleted: " + command.aggregateId()))
                .then();
    }
}
