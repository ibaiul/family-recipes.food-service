package eus.ibai.family.recipes.food.rm.domain.property;

import eus.ibai.family.recipes.food.exception.PropertyNotFoundException;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.PropertyEntityRepository;
import lombok.AllArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
class PropertyProjectionQueryHandler {

    private final PropertyEntityRepository propertyEntityRepository;

    @QueryHandler
    Flux<PropertyProjection> handle(FindAllPropertiesQuery query) {
        return propertyEntityRepository.findAll()
                .map(propertyEntity -> new PropertyProjection(propertyEntity.getId(), propertyEntity.getName()));
    }

    @QueryHandler
    Mono<PropertyProjection> handle(FindPropertyByIdQuery query) {
        return propertyEntityRepository.findById(query.propertyId())
                .switchIfEmpty(Mono.error(new PropertyNotFoundException("Property: " + query.propertyId())))
                .map(propertyEntity -> new PropertyProjection(propertyEntity.getId(), propertyEntity.getName()));
    }
}

