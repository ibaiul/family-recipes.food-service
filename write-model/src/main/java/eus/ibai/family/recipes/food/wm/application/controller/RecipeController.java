package eus.ibai.family.recipes.food.wm.application.controller;

import eus.ibai.family.recipes.food.exception.RecipeNotFoundException;
import eus.ibai.family.recipes.food.wm.application.dto.*;
import eus.ibai.family.recipes.food.wm.domain.file.InvalidFileException;
import eus.ibai.family.recipes.food.wm.domain.recipe.*;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.Optional;

@Validated
@RestController
@RequestMapping("/recipes")
@AllArgsConstructor
public class RecipeController {

    private RecipeService recipeService;

    @PostMapping
    public Mono<ResponseEntity<Void>> createRecipe(@Valid @RequestBody CreateRecipeDto dto, UriComponentsBuilder uriComponentsBuilder) {
        return recipeService.createRecipe(dto.recipeName())
                .map(id -> uriComponentsBuilder.path("/recipes/{id}").buildAndExpand(id).toUri())
                .map(uri -> ResponseEntity.created(uri).build());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateRecipe(@PathVariable("id") String recipeId, @Valid @RequestBody UpdateRecipeDto dto) {
        return recipeService.updateRecipe(recipeId, dto.recipeName(), dto.recipeLinks());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteRecipe(@PathVariable("id") String recipeId) {
        return recipeService.deleteRecipe(recipeId);
    }

    @PostMapping("/{id}/ingredients")
    public Mono<ResponseEntity<Void>> addIngredient(@PathVariable("id") String recipeId, @Valid @RequestBody AddRecipeIngredientDto dto, UriComponentsBuilder uriComponentsBuilder) {
        return recipeService.addRecipeIngredient(recipeId, dto.ingredientName())
                .map(ingredientId -> uriComponentsBuilder.path("/recipes/{id}/ingredients/{id}").buildAndExpand(recipeId, ingredientId).toUri())
                .map(uri -> ResponseEntity.created(uri).build());
    }

    @DeleteMapping("/{id}/ingredients/{id2}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeIngredient(@PathVariable("id") String recipeId, @PathVariable("id2") String ingredientId) {
        return recipeService.removeRecipeIngredient(recipeId, ingredientId);
    }

    @PostMapping("/{id}/tags")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> addTag(@PathVariable("id") String recipeId, @Valid @RequestBody AddRecipeTagDto dto) {
        return recipeService.addRecipeTag(recipeId, dto.tag());
    }

    @DeleteMapping("/{id}/tags")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeTag(@PathVariable("id") String recipeId, @NotBlank @RequestParam("name") String tag) {
        return recipeService.removeRecipeTag(recipeId, tag);
    }

    @PostMapping("/{id}/images")
    public Mono<ResponseEntity<String>> addRecipeImage(@PathVariable("id") String recipeId, @RequestHeader HttpHeaders headers, @RequestBody Flux<ByteBuffer> body, UriComponentsBuilder uriComponentsBuilder) {
        long length = headers.getContentLength();
        String mediaType = Optional.ofNullable(headers.getContentType())
                .map(MimeType::toString)
                .orElse("");
        return recipeService.addRecipeImage(recipeId, mediaType, length, body)
                .map(imageId -> uriComponentsBuilder.path("/recipes/{id}/images/{id}").buildAndExpand(recipeId, imageId).toUri())
                .map(uri -> ResponseEntity.created(uri).build());
    }

    @DeleteMapping("/{id}/images/{id2}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeImage(@PathVariable("id") String recipeId, @PathVariable("id2") String imageId) {
        return recipeService.removeRecipeImage(recipeId, imageId);
    }

    @ExceptionHandler({ RecipeNotFoundException.class, RecipeIngredientNotFoundException.class, RecipeTagNotFoundException.class, RecipeImageNotFoundException.class})
    public ResponseEntity<Void> handleNotFoundExceptions() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({ RecipeAlreadyExistsException.class, RecipeIngredientAlreadyAddedException.class })
    public ResponseEntity<Void> handleConflictExceptions() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @ExceptionHandler({ ConstraintViolationException.class, InvalidFileException.class })
    public ResponseEntity<ErrorResponse> handleBadRequestExceptions(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(exception.getMessage()));
    }
}
