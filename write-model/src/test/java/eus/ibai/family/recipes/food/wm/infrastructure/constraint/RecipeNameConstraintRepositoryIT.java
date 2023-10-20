package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RecipeNameConstraintRepositoryIT {

    @Autowired
    private RecipeNameConstraintRepository repository;

    @Test
    void should_insert_recipe_name_constraint_when_providing_id() {
        RecipeNameConstraintEntity expectedEntity = new RecipeNameConstraintEntity("recipeId", "recipeName");

        repository.save(new RecipeNameConstraintEntity(expectedEntity.getRecipeId(), expectedEntity.getRecipeName()));

        Optional<RecipeNameConstraintEntity> actualEntity = repository.findById(expectedEntity.getRecipeId());
        assertThat(actualEntity).contains(expectedEntity);
    }

    @Test
    void should_return_name_exists() {
        RecipeNameConstraintEntity entity = new RecipeNameConstraintEntity("recipeId", "recipeName");
        repository.save(entity);

        boolean nameExists = repository.nameExists("recipeName");

        assertThat(nameExists).isTrue();
    }

    @Test
    void should_return_name_does_not_exist() {
        boolean nameExists = repository.nameExists("recipeName");

        assertThat(nameExists).isFalse();
    }


    @Test
    void should_return_name_exists_for_another_recipe() {
        RecipeNameConstraintEntity entity = new RecipeNameConstraintEntity("anotherRecipeId", "recipeName");
        repository.save(entity);

        boolean nameExistsForAnotherRecipe = repository.nameExistsForAnotherRecipe("recipeId", "recipeName");

        assertThat(nameExistsForAnotherRecipe).isTrue();
    }

    @Test
    void should_return_name_does_not_exist_for_another_recipe_when_name_does_not_exist() {
        boolean nameExistsForAnotherRecipe = repository.nameExistsForAnotherRecipe("recipeId", "recipeName");

        assertThat(nameExistsForAnotherRecipe).isFalse();
    }

    @Test
    void should_return_name_does_not_exist_for_another_recipe_when_name_exists_for_same_recipe() {
        RecipeNameConstraintEntity entity = new RecipeNameConstraintEntity("recipeId", "recipeName");
        repository.save(entity);

        boolean nameExistsForAnotherRecipe = repository.nameExistsForAnotherRecipe("recipeId", "recipeName");

        assertThat(nameExistsForAnotherRecipe).isFalse();
    }
}