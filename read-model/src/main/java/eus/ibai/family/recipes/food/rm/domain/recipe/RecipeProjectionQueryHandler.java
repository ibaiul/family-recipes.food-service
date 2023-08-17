package eus.ibai.family.recipes.food.rm.domain.recipe;

import eus.ibai.family.recipes.food.exception.RecipeNotFoundException;
import eus.ibai.family.recipes.food.rm.infrastructure.model.RecipeEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.model.RecipeIngredientEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.IngredientEntityRepository;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.RecipeEntityRepository;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.RecipeIngredientEntityRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
@AllArgsConstructor
class RecipeProjectionQueryHandler {

    private final RecipeEntityRepository recipeEntityRepository;

    private final IngredientEntityRepository ingredientEntityRepository;

    private final RecipeIngredientEntityRepository recipeIngredientEntityRepository;

    @QueryHandler
    Mono<RecipeProjection> getById(FindRecipeByIdQuery query) {
        return recipeEntityRepository.findById(query.recipeId())
                .switchIfEmpty(Mono.error(new RecipeNotFoundException("Id: " + query.recipeId())))
                .flatMap(this::loadLazyRelations);
    }

    @QueryHandler
    Flux<RecipeProjection> getByLazy(FindRecipesByQuery query) {
        return query.getIngredientId()
                .map(recipeEntityRepository::findByIngredientId)
                .orElseGet(() -> query.getPropertyId()
                        .map(recipeEntityRepository::findByPropertyId)
                        .orElseGet(recipeEntityRepository::findAll))
                .map(this::toDomainObjectLazy);
    }

    private Mono<RecipeProjection> loadLazyRelations(RecipeEntity recipeEntityLazy) {
        return recipeIngredientEntityRepository.findByRecipeId(recipeEntityLazy.getId())
                .collectMap(RecipeIngredientEntity::getIngredientId, identity())
                .flatMap(recipeProperties -> ingredientEntityRepository.findAllById(recipeProperties.keySet())
                        .reduce(recipeEntityLazy, RecipeEntity::addIngredient)
                        .map(recipeEntity -> toDomainObject(recipeEntity, recipeProperties)));
    }

    private RecipeProjection toDomainObjectLazy(RecipeEntity recipeEntity) {
        return toDomainObject(recipeEntity, null);
    }

    private RecipeProjection toDomainObject(RecipeEntity recipeEntity, Map<String, RecipeIngredientEntity> recipePropertiesMap) {
        Set<RecipeIngredientProjection> recipeProperties = Optional.ofNullable(recipeEntity.getIngredients())
                .map(ingredientEntities -> ingredientEntities.stream()
                        .map(ingredientEntity -> new RecipeIngredientProjection(ingredientEntity.getId(), ingredientEntity.getName(), recipePropertiesMap.get(ingredientEntity.getId()).getAddedOn()))
                        .collect(Collectors.toSet()))
                .orElseGet(Collections::emptySet);
        return new RecipeProjection(recipeEntity.getId(), recipeEntity.getName(), recipeEntity.getLinks(), recipeProperties);
    }
}
