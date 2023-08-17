package eus.ibai.family.recipes.food.rm.infrastructure.repository;

import eus.ibai.family.recipes.food.rm.infrastructure.model.IngredientPropertyEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.LocalDateTime;

public interface IngredientPropertyEntityRepository extends ReactiveCrudRepository<IngredientPropertyEntity, Tuple2<String, String>> {

    @Query("INSERT INTO ingredient_property VALUES(:ingredientId, :propertyId, :addedOn)")
    Mono<IngredientPropertyEntity> saveNew(@Param("ingredientId") String ingredientId, @Param("propertyId") String propertyId, @Param("addedOn") LocalDateTime addedOn);

    Flux<IngredientPropertyEntity> findByIngredientId(String ingredientId);

    Mono<Long> deleteByIngredientIdAndPropertyId(String ingredientId, String propertyId);

    Mono<Long> deleteByIngredientId(String ingredientId);
}
