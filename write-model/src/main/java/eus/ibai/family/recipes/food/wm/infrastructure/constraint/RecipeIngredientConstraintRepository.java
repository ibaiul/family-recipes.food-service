package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeIngredientConstraintRepository extends JpaRepository<RecipeIngredientConstraintEntity, RecipeIngredientId> {

    @Query(value = "SELECT CAST(CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END AS BIT) FROM recipe_ingredient_constraint WHERE ingredient_id = :ingredientId", nativeQuery = true)
    boolean ingredientExists(@Param("ingredientId") String ingredientId);

    int deleteByRecipeId(String recipeId);
}
