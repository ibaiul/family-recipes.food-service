package eus.ibai.family.recipes.food.rm.application.controller;

import eus.ibai.family.recipes.food.exception.IngredientNotFoundException;
import eus.ibai.family.recipes.food.rm.application.dto.BasicIngredientDto;
import eus.ibai.family.recipes.food.rm.application.dto.IngredientDto;
import eus.ibai.family.recipes.food.rm.domain.ingredient.FindIngredientByIdQuery;
import eus.ibai.family.recipes.food.rm.domain.ingredient.FindIngredientsByQuery;
import eus.ibai.family.recipes.food.rm.domain.ingredient.IngredientProjection;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.hibernate.validator.constraints.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/ingredients")
@AllArgsConstructor
public class IngredientController {

    private final ReactorQueryGateway queryGateway;

    @GetMapping("/{id}")
    public Mono<IngredientDto> getById(@PathVariable("id") @UUID String ingredientId) {
        return queryGateway.streamingQuery(new FindIngredientByIdQuery(ingredientId), IngredientProjection.class)
                .map(IngredientDto::fromProjection)
                .next();
    }

    @GetMapping
    public Flux<BasicIngredientDto> getBy(@RequestParam(value = "propertyId", required = false) @UUID String propertyId) {
        return queryGateway.streamingQuery(new FindIngredientsByQuery(propertyId), IngredientProjection.class)
                    .map(BasicIngredientDto::fromProjection);
    }

    @ExceptionHandler({ IngredientNotFoundException.class })
    public ResponseEntity<Void> handleNotFoundExceptions() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({ ConstraintViolationException.class })
    public ResponseEntity<Void> handleBadRequestExceptions() {
        return ResponseEntity.badRequest().build();
    }
}
