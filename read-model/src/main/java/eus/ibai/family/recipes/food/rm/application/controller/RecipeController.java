package eus.ibai.family.recipes.food.rm.application.controller;

import eus.ibai.family.recipes.food.exception.RecipeNotFoundException;
import eus.ibai.family.recipes.food.rm.application.dto.BasicRecipeDto;
import eus.ibai.family.recipes.food.rm.application.dto.BasicRecipeTagDto;
import eus.ibai.family.recipes.food.rm.application.dto.RecipeDto;
import eus.ibai.family.recipes.food.rm.domain.file.FileStorage;
import eus.ibai.family.recipes.food.rm.domain.recipe.FindRecipeByIdQuery;
import eus.ibai.family.recipes.food.rm.domain.recipe.FindRecipeTagsQuery;
import eus.ibai.family.recipes.food.rm.domain.recipe.FindRecipesByQuery;
import eus.ibai.family.recipes.food.rm.domain.recipe.RecipeProjection;
import eus.ibai.family.recipes.food.rm.infrastructure.config.RecipeProperties;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.hibernate.validator.constraints.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;

@Validated
@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final ReactorQueryGateway queryGateway;

    private final FileStorage fileStorage;

    private final RecipeProperties recipeProperties;

    @GetMapping("/{id}")
    public Mono<RecipeDto> getById(@PathVariable("id") @UUID String recipeId) {
        return queryGateway.streamingQuery(new FindRecipeByIdQuery(recipeId), RecipeProjection.class)
                .map(RecipeDto::fromProjection)
                .next();
    }

    @GetMapping
    public Flux<BasicRecipeDto> getBy(@RequestParam(value = "ingredientId", required = false) @UUID String ingredientId,
                                      @RequestParam(value = "propertyId", required = false) @UUID String propertyId,
                                      @RequestParam(value = "tag", required = false) String tag) {
        return queryGateway.streamingQuery(new FindRecipesByQuery(ingredientId, propertyId, tag), RecipeProjection.class)
                .map(BasicRecipeDto::fromProjection);
    }

    @GetMapping("/tags")
    public Flux<BasicRecipeTagDto> getAllTags() {
        return queryGateway.streamingQuery(new FindRecipeTagsQuery(), String.class)
                .map(BasicRecipeTagDto::new);
    }

    @GetMapping(path = "/{id}/images/{imageId}")
    public Mono<ResponseEntity<ByteBuffer>> getRecipeImage(@PathVariable("id") @UUID String recipeId, @PathVariable("imageId") String imageId) {
        return queryGateway.streamingQuery(new FindRecipeByIdQuery(recipeId), RecipeProjection.class)
                .next()
                .filter(recipeProjection -> recipeProjection.images().contains(imageId))
                .flatMap(recipeProjection -> fileStorage.retrieveFile(recipeProperties.getImages().storagePath() + imageId))
                .map(file -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_TYPE, file.contentType())
                        .header(HttpHeaders.CONTENT_LENGTH, Long.toString(file.contentLength()))
                        .body(file.content()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @ExceptionHandler({ RecipeNotFoundException.class, FileNotFoundException.class})
    public ResponseEntity<Void> handleNotFoundExceptions() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({ ConstraintViolationException.class })
    public ResponseEntity<Void> handleBadRequestExceptions() {
        return ResponseEntity.badRequest().build();
    }
}
