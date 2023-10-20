package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IngredientNameConstraintRepository extends JpaRepository<IngredientNameConstraintEntity, String> {

    @Query(value = "SELECT CAST(CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END AS BIT) FROM ingredient_name_constraint WHERE ingredient_name = :ingredientName", nativeQuery = true)
    boolean nameExists(@Param("ingredientName") String ingredientName);

    @Query(value = "SELECT CAST(CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END AS BIT) FROM ingredient_name_constraint WHERE ingredient_name = :ingredientName AND ingredient_id != :ingredientId", nativeQuery = true)
    boolean nameExistsForAnotherIngredient(@Param("ingredientId") String ingredientId, @Param("ingredientName") String ingredientName);

    Optional<IngredientNameConstraintEntity> findByIngredientName(String ingredientName);
}
