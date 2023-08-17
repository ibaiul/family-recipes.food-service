package eus.ibai.family.recipes.food.rm.infrastructure.repository;

import eus.ibai.family.recipes.food.rm.infrastructure.model.RecipeIngredientEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.LocalDateTime;

public interface RecipeIngredientEntityRepository extends ReactiveCrudRepository<RecipeIngredientEntity, Tuple2<String, String>> {

    @Query("INSERT INTO recipe_ingredient VALUES(:recipeId, :ingredientId, :addedOn)")
    Mono<RecipeIngredientEntity> saveNew(@Param("recipeId") String recipeId, @Param("ingredientId") String ingredientId, @Param("addedOn") LocalDateTime addedOn);

    Flux<RecipeIngredientEntity> findByRecipeId(String recipeId);

    Mono<Long> deleteByRecipeIdAndIngredientId(String recipeId, String ingredientId);

    Mono<Long> deleteByRecipeId(String recipeId);
}
