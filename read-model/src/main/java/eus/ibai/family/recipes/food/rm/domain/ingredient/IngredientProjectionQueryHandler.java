package eus.ibai.family.recipes.food.rm.domain.ingredient;

import eus.ibai.family.recipes.food.exception.IngredientNotFoundException;
import eus.ibai.family.recipes.food.rm.infrastructure.model.IngredientEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.model.IngredientPropertyEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.IngredientEntityRepository;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.IngredientPropertyEntityRepository;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.PropertyEntityRepository;
import lombok.AllArgsConstructor;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Component
@AllArgsConstructor
class IngredientProjectionQueryHandler {

    private final IngredientEntityRepository ingredientEntityRepository;

    private final PropertyEntityRepository propertyEntityRepository;

    private final IngredientPropertyEntityRepository ingredientPropertyEntityRepository;

    @QueryHandler
    Mono<IngredientProjection> getById(FindIngredientByIdQuery query) {
        return ingredientEntityRepository.findById(query.ingredientId())
                .switchIfEmpty(Mono.error(new IngredientNotFoundException("Id: " + query.ingredientId())))
                .flatMap(this::loadLazyRelations);
    }

    @QueryHandler
    Flux<IngredientProjection> getByLazy(FindIngredientsByQuery query) {
        return query.getPropertyId()
                .map(ingredientEntityRepository::findByPropertyId)
                .orElseGet(ingredientEntityRepository::findAll)
                .map(this::toDomainObjectLazy);
    }

    private Mono<IngredientProjection> loadLazyRelations(IngredientEntity ingredientEntityLazy) {
        return ingredientPropertyEntityRepository.findByIngredientId(ingredientEntityLazy.getId())
                .collectMap(IngredientPropertyEntity::getPropertyId, identity())
                .flatMap(ingredientProperties -> propertyEntityRepository.findAllById(ingredientProperties.keySet())
                        .reduce(ingredientEntityLazy, IngredientEntity::addProperty)
                        .map(ingredientEntity -> toDomainObject(ingredientEntity, ingredientProperties)));
    }

    private IngredientProjection toDomainObjectLazy(IngredientEntity ingredientEntity) {
        return toDomainObject(ingredientEntity, null);
    }

    private IngredientProjection toDomainObject(IngredientEntity ingredientEntity, Map<String, IngredientPropertyEntity> ingredientPropertiesMap) {
        Set<IngredientPropertyProjection> ingredientProperties = Optional.ofNullable(ingredientEntity.getProperties())
                .map(propertyEntities -> propertyEntities.stream()
                        .map(propertyEntity -> new IngredientPropertyProjection(propertyEntity.getId(), propertyEntity.getName(), ingredientPropertiesMap.get(propertyEntity.getId()).getAddedOn()))
                        .collect(Collectors.toSet()))
                .orElseGet(Collections::emptySet);
        return new IngredientProjection(ingredientEntity.getId(), ingredientEntity.getName(), ingredientProperties);
    }
}
