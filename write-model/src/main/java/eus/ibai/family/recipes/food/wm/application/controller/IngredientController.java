package eus.ibai.family.recipes.food.wm.application.controller;

import eus.ibai.family.recipes.food.exception.IngredientNotFoundException;
import eus.ibai.family.recipes.food.wm.application.dto.AddIngredientPropertyDto;
import eus.ibai.family.recipes.food.wm.application.dto.CreateIngredientDto;
import eus.ibai.family.recipes.food.wm.application.dto.UpdateIngredientDto;
import eus.ibai.family.recipes.food.wm.domain.ingredient.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ingredients")
@AllArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @PostMapping
    public Mono<ResponseEntity<Void>> createIngredient(@Valid @RequestBody CreateIngredientDto dto, UriComponentsBuilder uriComponentsBuilder) {
        return ingredientService.createIngredient(dto.ingredientName())
                .map(id -> uriComponentsBuilder.path("/ingredients/{id}").buildAndExpand(id).toUri())
                .map(uri -> ResponseEntity.created(uri).build());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateIngredient(@PathVariable("id") String ingredientId, @Valid @RequestBody UpdateIngredientDto dto) {
        return ingredientService.updateIngredient(ingredientId, dto.ingredientName());
    }

    @PostMapping("/{id}/properties")
    public Mono<ResponseEntity<Void>> addIngredientProperty(@PathVariable("id") String ingredientId, @Valid @RequestBody AddIngredientPropertyDto dto, UriComponentsBuilder uriComponentsBuilder) {
        return ingredientService.addIngredientProperty(ingredientId, dto.propertyName())
                .map(propertyId -> uriComponentsBuilder.path("/ingredients/{id}/properties/{id}").buildAndExpand(ingredientId, propertyId).toUri())
                .map(uri -> ResponseEntity.created(uri).build());
    }

    @DeleteMapping("/{id}/properties/{id2}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> removeIngredientProperty(@PathVariable("id") String ingredientId, @PathVariable("id2") String propertyId) {
        return ingredientService.removeIngredientProperty(ingredientId, propertyId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteIngredient(@PathVariable("id") String ingredientId) {
        return ingredientService.deleteIngredient(ingredientId);
    }

    @ExceptionHandler({ IngredientNotFoundException.class, IngredientPropertyNotFoundException.class })
    public ResponseEntity<Void> handleNotFoundExceptions() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({ IngredientAlreadyExistsException.class, IngredientPropertyAlreadyAddedException.class, IngredientAttachedToRecipeException.class })
    public ResponseEntity<Void> handleConflictExceptions() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
