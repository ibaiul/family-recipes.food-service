package eus.ibai.family.recipes.food.wm.application.controller;

import eus.ibai.family.recipes.food.exception.RecipeNotFoundException;
import eus.ibai.family.recipes.food.wm.application.dto.AddRecipeIngredientDto;
import eus.ibai.family.recipes.food.wm.application.dto.AddRecipeTagDto;
import eus.ibai.family.recipes.food.wm.application.dto.CreateRecipeDto;
import eus.ibai.family.recipes.food.wm.application.dto.UpdateRecipeDto;
import eus.ibai.family.recipes.food.wm.domain.recipe.*;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

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

    @ExceptionHandler({ RecipeNotFoundException.class, RecipeIngredientNotFoundException.class, RecipeTagNotFoundException.class})
    public ResponseEntity<Void> handleNotFoundExceptions() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({ RecipeAlreadyExistsException.class, RecipeIngredientAlreadyAddedException.class })
    public ResponseEntity<Void> handleConflictExceptions() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @ExceptionHandler({ ConstraintViolationException.class })
    public ResponseEntity<Void> handleBadRequestExceptions() {
        return ResponseEntity.badRequest().build();
    }
}
