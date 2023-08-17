package eus.ibai.family.recipes.food.wm.application.controller;

import eus.ibai.family.recipes.food.exception.PropertyNotFoundException;
import eus.ibai.family.recipes.food.wm.application.dto.CreatePropertyDto;
import eus.ibai.family.recipes.food.wm.application.dto.UpdatePropertyDto;
import eus.ibai.family.recipes.food.wm.domain.property.PropertyAlreadyExistsException;
import eus.ibai.family.recipes.food.wm.domain.property.PropertyAttachedToIngredientException;
import eus.ibai.family.recipes.food.wm.domain.property.PropertyService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/properties")
@AllArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    public Mono<ResponseEntity<Void>> createProperty(@Valid @RequestBody CreatePropertyDto dto, UriComponentsBuilder uriComponentsBuilder) {
        return propertyService.createProperty(dto.propertyName())
                .map(id -> uriComponentsBuilder.path("/properties/{id}").buildAndExpand(id).toUri())
                .map(uri -> ResponseEntity.created(uri).build());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateProperty(@PathVariable("id") String propertyId, @Valid @RequestBody UpdatePropertyDto dto) {
        return propertyService.updateProperty(propertyId, dto.propertyName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProperty(@PathVariable("id") String propertyId) {
        return propertyService.deleteProperty(propertyId);
    }

    @ExceptionHandler({ PropertyNotFoundException.class })
    public ResponseEntity<Void> handleNotFoundExceptions() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({ PropertyAlreadyExistsException.class, PropertyAttachedToIngredientException.class })
    public ResponseEntity<Void> handleConflictExceptions() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
