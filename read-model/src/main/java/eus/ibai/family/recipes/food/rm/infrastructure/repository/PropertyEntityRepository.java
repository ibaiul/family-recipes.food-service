package eus.ibai.family.recipes.food.rm.infrastructure.repository;

import eus.ibai.family.recipes.food.rm.infrastructure.model.PropertyEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PropertyEntityRepository extends ReactiveCrudRepository<PropertyEntity, String> {

    @Query("INSERT INTO property VALUES(:id, :name)")
    Mono<Void> saveNew(@Param("id") String id, @Param("name") String name);
}
