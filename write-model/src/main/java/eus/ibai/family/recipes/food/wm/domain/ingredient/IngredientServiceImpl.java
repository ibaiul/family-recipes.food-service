package eus.ibai.family.recipes.food.wm.domain.ingredient;

import eus.ibai.family.recipes.food.exception.IngredientNotFoundException;
import eus.ibai.family.recipes.food.wm.domain.command.AggregateCommand;
import eus.ibai.family.recipes.food.wm.domain.property.CreatePropertyCommand;
import eus.ibai.family.recipes.food.wm.infrastructure.exception.DownstreamConnectivityException;
import lombok.AllArgsConstructor;
import org.axonframework.eventsourcing.AggregateDeletedException;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static eus.ibai.family.recipes.food.util.Utils.generateId;

/**
 * Domain service to enforce invariants out of the scope of a single aggregate.
 */

@Service
@AllArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final ReactorCommandGateway commandGateway;

    private final IngredientConstraintRepository ingredientConstraintRepository;

    @Override
    public Mono<String> createIngredient(String ingredientName) {
        return Mono.defer(() -> Mono.just(ingredientConstraintRepository.nameExists(ingredientName)))
                .onErrorMap(t -> new DownstreamConnectivityException("Could not determine if property name exists.", t))
                .filter(exists -> exists)
                .handle((ingredient, sink) -> sink.error(new IngredientAlreadyExistsException(ingredientName)))
                .switchIfEmpty(Mono.defer(() -> Mono.just(new CreateIngredientCommand(generateId(), ingredientName))))
                .flatMap(commandGateway::send);
    }

    @Override
    public Mono<Void> updateIngredient(String ingredientId, String ingredientName) {
        return Mono.defer(() -> Mono.just(ingredientConstraintRepository.anotherIngredientContainsName(ingredientId, ingredientName)))
                .onErrorMap(t -> new DownstreamConnectivityException("Could not determine if property name exists.", t))
                .filter(exists -> exists)
                .handle((ingredient, sink) -> sink.error(new IngredientAlreadyExistsException(ingredientName)))
                .switchIfEmpty(Mono.defer(() -> send(new UpdateIngredientCommand(ingredientId, ingredientName))))
                .then();
    }

    @Override
    public Mono<Void> deleteIngredient(String ingredientId) {
        return Mono.defer(() -> Mono.just(ingredientConstraintRepository.isIngredientBoundToRecipes(ingredientId)))
                .onErrorMap(t -> new DownstreamConnectivityException("Could not determine if ingredient bound to any recipe.", t))
                .filter(isBound -> !isBound)
                .switchIfEmpty(Mono.error(new IngredientAttachedToRecipeException(ingredientId)))
                .flatMap(isNotBound -> Mono.defer(() -> send(new DeleteIngredientCommand(ingredientId))));
    }

    @Override
    public Mono<String> addIngredientProperty(String ingredientId, String propertyName) {
        return Mono.defer(() -> Mono.just(ingredientConstraintRepository.retrievePropertyId(propertyName)))
                .onErrorMap(t -> new DownstreamConnectivityException("Could not determine if property exists.", t))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .switchIfEmpty(Mono.just(generateId())
                        .flatMap(propertyId -> send(new CreatePropertyCommand(propertyId, propertyName))
                                .thenReturn(propertyId)))
                .flatMap(propertyId -> send(new AddIngredientPropertyCommand(ingredientId, propertyId))
                        .thenReturn(propertyId));
    }

    @Override
    public Mono<Void> removeIngredientProperty(String ingredientId, String propertyId) {
        return send(new RemoveIngredientPropertyCommand(ingredientId, propertyId));
    }

    private Mono<Void> send(AggregateCommand<String> command) {
        return commandGateway.send(command)
                .onErrorMap(AggregateNotFoundException.class, t -> new IngredientNotFoundException(command.aggregateId()))
                .onErrorMap(AggregateDeletedException.class, t -> new IngredientNotFoundException("Ingredient is deleted: " + command.aggregateId()))
                .then();
    }
}
