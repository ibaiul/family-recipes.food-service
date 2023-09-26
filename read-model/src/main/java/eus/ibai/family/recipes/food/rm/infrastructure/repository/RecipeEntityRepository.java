package eus.ibai.family.recipes.food.rm.infrastructure.repository;

import eus.ibai.family.recipes.food.rm.infrastructure.model.RecipeEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RecipeEntityRepository extends R2dbcRepository<RecipeEntity, String> {

    @Query("INSERT INTO recipe (id, name)  VALUES(:id, :name)")
    Mono<Void> saveNew(@Param("id") String id, @Param("name") String name);

    @Query("SELECT * FROM recipe r JOIN recipe_ingredient ri ON r.id = ri.recipe_id WHERE ri.ingredient_id = :ingredientId")
    Flux<RecipeEntity> findByIngredientId(@Param("ingredientId") String ingredientId);

    @Query("SELECT * FROM recipe r JOIN recipe_ingredient ri ON r.id = ri.recipe_id JOIN ingredient_property ip ON ri.ingredient_id = ip.ingredient_id WHERE ip.property_id = :propertyId")
    Flux<RecipeEntity> findByPropertyId(@Param("propertyId") String propertyId);

    @Query("SELECT * FROM recipe r WHERE :tag = ANY(r.tags)")
    Flux<RecipeEntity> findByTag(@Param("tag") String tag);

    @Query("SELECT DISTINCT UNNEST(r.tags) FROM Recipe r")
    Flux<String> findAllTags();
}
