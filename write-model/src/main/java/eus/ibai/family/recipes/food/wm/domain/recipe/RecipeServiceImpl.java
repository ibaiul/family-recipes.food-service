package eus.ibai.family.recipes.food.wm.domain.recipe;

import eus.ibai.family.recipes.food.exception.RecipeNotFoundException;
import eus.ibai.family.recipes.food.wm.domain.command.AggregateCommand;
import eus.ibai.family.recipes.food.wm.domain.command.RemoteHandlingExceptionMapper;
import eus.ibai.family.recipes.food.wm.domain.file.FileStorage;
import eus.ibai.family.recipes.food.wm.domain.file.InvalidFileException;
import eus.ibai.family.recipes.food.wm.domain.ingredient.CreateIngredientCommand;
import eus.ibai.family.recipes.food.wm.infrastructure.exception.DownstreamConnectivityException;
import lombok.RequiredArgsConstructor;
import org.axonframework.eventsourcing.AggregateDeletedException;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.messaging.RemoteHandlingException;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static eus.ibai.family.recipes.food.util.Utils.generateId;

/**
 * Domain service to enforce invariants out of the scope of a single aggregate.
 */

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final ReactorCommandGateway commandGateway;

    private final RecipeConstraintRepository recipeConstraintRepository;

    private final FileStorage fileStorage;

    private final RecipeProperties properties;

    @Override
    public Mono<String> createRecipe(String recipeName) {
        return Mono.defer(() -> Mono.just(recipeConstraintRepository.nameExists(recipeName)))
                .onErrorMap(t -> new DownstreamConnectivityException("Could not determine if ingredient name exists.", t))
                .filter(exists -> exists)
                .handle((recipe, sink) -> sink.error(new RecipeAlreadyExistsException(recipeName)))
                .switchIfEmpty(Mono.defer(() -> Mono.just(new CreateRecipeCommand(generateId(), recipeName))))
                .cast(AggregateCommand.class)
                .flatMap(this::send)
                .cast(String.class);
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
        return send(new DeleteRecipeCommand(recipeId)).then();
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
        return send(new RemoveRecipeIngredientCommand(recipeId, ingredientId)).then();
    }

    @Override
    public Mono<Void> addRecipeTag(String recipeId, String tag) {
        return send(new AddRecipeTagCommand(recipeId, tag)).then();
    }

    @Override
    public Mono<Void> removeRecipeTag(String recipeId, String tag) {
        return send(new RemoveRecipeTagCommand(recipeId, tag)).then();
    }

    @Override
    public Mono<String> addRecipeImage(String recipeId, String mediaType, long length, Flux<ByteBuffer> fileContent) {
        return validateImage(mediaType, length)
                .thenReturn(recipeConstraintRepository.idExists(recipeId))
                .filter(recipeExists -> recipeExists)
                .flatMap(recipeExists -> {
                    Map<String, String> metadata = Map.of("entity", "recipe", "entityId", recipeId);
                    return fileStorage.storeFile(properties.getImages().storagePath(), mediaType, length, fileContent, metadata);
                })
                .flatMap(imageId -> send(new AddRecipeImageCommand(recipeId, imageId))
                        .thenReturn(imageId))
                .switchIfEmpty(Mono.error(new RecipeNotFoundException("")));
    }

    @Override
    public Mono<Void> removeRecipeImage(String recipeId, String imageId) {
        return send(new RemoveRecipeImageCommand(recipeId, imageId))
                .then(Mono.defer(() -> fileStorage.deleteFile("recipes/images/" + imageId)))
                .onErrorComplete(IOException.class);
    }

    private Mono<Object> send(AggregateCommand<String> command) {
        return commandGateway.send(command)
                .onErrorMap(AggregateNotFoundException.class, t -> new RecipeNotFoundException(command.aggregateId()))
                .onErrorMap(AggregateDeletedException.class, t -> new RecipeNotFoundException("Recipe is deleted: " + command.aggregateId()))
                .onErrorMap(t -> t.getCause() instanceof RemoteHandlingException, new RemoteHandlingExceptionMapper());
    }

    private Mono<Void> validateImage(String mediaType, long length) {
        RecipeProperties.ImageProperties imageProperties = properties.getImages();
        if (length < imageProperties.minSize() || length > imageProperties.maxSize()) {
            return Mono.error(new InvalidFileException("File size must be between %d - %d KB. Size: %d".formatted(imageProperties.minSize(), imageProperties.maxSize(), length)));
        }
        if (!imageProperties.mediaTypes().contains(mediaType)) {
            return Mono.error(new InvalidFileException("File type must be an image. Type: " + mediaType));
        }
        return Mono.empty();
    }
}
