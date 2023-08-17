package eus.ibai.family.recipes.food.rm.application.controller;

import eus.ibai.family.recipes.food.exception.PropertyNotFoundException;
import eus.ibai.family.recipes.food.rm.application.dto.BasicPropertyDto;
import eus.ibai.family.recipes.food.rm.domain.property.FindAllPropertiesQuery;
import eus.ibai.family.recipes.food.rm.domain.property.FindPropertyByIdQuery;
import eus.ibai.family.recipes.food.rm.domain.property.PropertyProjection;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.hibernate.validator.constraints.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Validated
@RestController
@RequestMapping("/properties")
@AllArgsConstructor
public class PropertyController {

    private final ReactorQueryGateway queryGateway;

    @GetMapping
    public Flux<BasicPropertyDto> getAll() {
        log.debug("Getting all properties");
        return queryGateway.streamingQuery(new FindAllPropertiesQuery(), PropertyProjection.class)
                .map(BasicPropertyDto::fromProjection);
    }

    @GetMapping("/{id}")
    public Mono<BasicPropertyDto> getById(@PathVariable("id") @UUID String propertyId) {
        log.debug("Getting property by id {}", propertyId);
        return queryGateway.streamingQuery(new FindPropertyByIdQuery(propertyId), PropertyProjection.class)
                .map(BasicPropertyDto::fromProjection)
                .next();
    }

    @ExceptionHandler({ PropertyNotFoundException.class })
    public ResponseEntity<Void> handleNotFoundExceptions() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler({ ConstraintViolationException.class })
    public ResponseEntity<Void> handleBadRequestExceptions() {
        return ResponseEntity.badRequest().build();
    }
}
