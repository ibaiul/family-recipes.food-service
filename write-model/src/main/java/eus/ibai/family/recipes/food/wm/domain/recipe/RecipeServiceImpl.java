package eus.ibai.family.recipes.food.wm.domain.recipe;

import eus.ibai.family.recipes.food.exception.RecipeNotFoundException;
import eus.ibai.family.recipes.food.wm.domain.command.AggregateCommand;
import eus.ibai.family.recipes.food.wm.domain.ingredient.CreateIngredientCommand;
import eus.ibai.family.recipes.food.wm.infrastructure.exception.DownstreamConnectivityException;
import lombok.AllArgsConstructor;
import org.axonframework.eventsourcing.AggregateDeletedException;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;

import static eus.ibai.family.recipes.food.util.Utils.generateId;

/**
 * Domain service to enforce invariants out of the scope of a single aggregate.
 */

@Service
@AllArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final ReactorCommandGateway commandGateway;

    private final RecipeConstraintRepository recipeConstraintRepository;

    @Override
    public Mono<String> createRecipe(String recipeName) {
        return Mono.defer(() -> Mono.just(recipeConstraintRepository.nameExists(recipeName)))
                .onErrorMap(t -> new DownstreamConnectivityException("Could not determine if ingredient name exists.", t))
                .filter(exists -> exists)
                .handle((recipe, sink) -> sink.error(new RecipeAlreadyExistsException(recipeName)))
                .switchIfEmpty(Mono.defer(() -> Mono.just(new CreateRecipeCommand(generateId(), recipeName))))
                .flatMap(commandGateway::send);
    }

    @Override
    public Mono<Void> updateRecipe(String recipeId, String recipeName, Set<String> links) {
        return Mono.defer(() -> Mono.just(recipeConstraintRepository.anotherRecipeContainsName(recipeId, recipeName)))
                .onErrorMap(t -> new DownstreamConnectivityException("Could not determine if ingredient name exists.", t))
                .filter(exists -> exists)
                .handle((recipe, sink) -> sink.error(new RecipeAlreadyExistsException(recipeName)))
                .switchIfEmpty(Mono.defer(() -> send(new UpdateRecipeCommand(recipeId, recipeName, links))))
                .then();
    }

    @Override
    public Mono<Void> deleteRecipe(String recipeId) {
        return send(new DeleteRecipeCommand(recipeId));
    }

    @Override
    public Mono<String> addRecipeIngredient(String recipeId, String ingredientName) {
        return Mono.defer(() -> Mono.just(recipeConstraintRepository.retrieveIngredientId(ingredientName)))
                .onErrorMap(t -> new DownstreamConnectivityException("Could not determine if ingredient exists.", t))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .switchIfEmpty(Mono.just(generateId())
                        .flatMap(ingredientId -> send(new CreateIngredientCommand(ingredientId, ingredientName))
                                .thenReturn(ingredientId)))
                .flatMap(ingredientId -> send(new AddRecipeIngredientCommand(recipeId, ingredientId))
                        .thenReturn(ingredientId));
    }

    @Override
    public Mono<Void> removeRecipeIngredient(String recipeId, String ingredientId) {
        return send(new RemoveRecipeIngredientCommand(recipeId, ingredientId));
    }

    @Override
    public Mono<Void> addRecipeTag(String recipeId, String tag) {
        return send(new AddRecipeTagCommand(recipeId, tag));
    }

    @Override
    public Mono<Void> removeRecipeTag(String recipeId, String tag) {
        return send(new RemoveRecipeTagCommand(recipeId, tag));
    }

    private Mono<Void> send(AggregateCommand<String> command) {
        return commandGateway.send(command)
                .onErrorMap(AggregateNotFoundException.class, t -> new RecipeNotFoundException(command.aggregateId()))
                .onErrorMap(AggregateDeletedException.class, t -> new RecipeNotFoundException("Recipe is deleted: " + command.aggregateId()))
                .then();
    }
}
