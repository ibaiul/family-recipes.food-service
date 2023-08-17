package eus.ibai.family.recipes.food.rm.infrastructure.repository;

import eus.ibai.family.recipes.food.rm.infrastructure.model.IngredientEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IngredientEntityRepository extends ReactiveCrudRepository<IngredientEntity, String> {

    @Query("INSERT INTO ingredient VALUES(:id, :name)")
    Mono<Void> saveNew(@Param("id") String id, @Param("name") String name);

    @Query("SELECT i.* FROM ingredient i JOIN ingredient_property ip ON i.id = ip.ingredient_id WHERE ip.property_id = :propertyId")
    Flux<IngredientEntity> findByPropertyId(@Param("propertyId") String propertyId);
}
