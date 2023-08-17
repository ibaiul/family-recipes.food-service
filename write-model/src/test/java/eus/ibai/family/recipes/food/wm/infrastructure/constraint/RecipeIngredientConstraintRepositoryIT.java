package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class RecipeIngredientConstraintRepositoryIT {

    @Autowired
    private RecipeNameConstraintRepository recipeRepository;

    @Autowired
    private IngredientNameConstraintRepository ingredientRepository;

    @Autowired
    private RecipeIngredientConstraintRepository recipeIngredientRepository;

    @Test
    void should_return_recipe_ingredient_exists_by_ingredient_id() {
        RecipeNameConstraintEntity recipeEntity = new RecipeNameConstraintEntity("recipeId", "recipeName");
        recipeRepository.save(recipeEntity);
        IngredientNameConstraintEntity ingredientEntity = new IngredientNameConstraintEntity("ingredientId", "ingredientName");
        ingredientRepository.save(ingredientEntity);
        RecipeIngredientConstraintEntity recipeIngredientEntity = new RecipeIngredientConstraintEntity("recipeId", "ingredientId");
        recipeIngredientRepository.save(recipeIngredientEntity);

        boolean ingredientExists = recipeIngredientRepository.ingredientExists("ingredientId");

        assertThat(ingredientExists).isTrue();
    }

    @Test
    void should_return_recipe_ingredient_does_not_exist_by_ingredient_id() {
        boolean ingredientExists = recipeIngredientRepository.ingredientExists("ingredientId");

        assertThat(ingredientExists).isFalse();
    }
}