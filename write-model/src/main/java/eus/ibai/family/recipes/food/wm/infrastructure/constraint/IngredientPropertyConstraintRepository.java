package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngredientPropertyConstraintRepository extends JpaRepository<IngredientPropertyConstraintEntity, IngredientPropertyId> {

    @Query(value = "SELECT CAST(CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END AS BIT) FROM ingredient_property_constraint WHERE property_id = :propertyId", nativeQuery = true)
    boolean propertyExists(@Param("propertyId") String propertyId);

    int deleteByIngredientId(String ingredientId);
}
