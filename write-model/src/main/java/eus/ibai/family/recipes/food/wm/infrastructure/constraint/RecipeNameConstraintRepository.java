package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeNameConstraintRepository extends JpaRepository<RecipeNameConstraintEntity, String> {

    @Modifying
    @Query(value = "INSERT INTO recipe_name_constraint VALUES (:recipeId, :recipeName)", nativeQuery = true)
    int insert(@Param("recipeId") String recipeId, @Param("recipeName") String recipeName);

    @Query(value = "SELECT CAST(CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END AS BIT) FROM recipe_name_constraint WHERE recipe_name = :recipeName", nativeQuery = true)
    boolean nameExists(@Param("recipeName") String recipeName);

    @Query(value = "SELECT CAST(CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END AS BIT) FROM recipe_name_constraint WHERE recipe_name = :recipeName AND recipe_id != :recipeId", nativeQuery = true)
    boolean nameExistsForAnotherRecipe(@Param("recipeId") String recipeId, @Param("recipeName") String recipeName);
}
