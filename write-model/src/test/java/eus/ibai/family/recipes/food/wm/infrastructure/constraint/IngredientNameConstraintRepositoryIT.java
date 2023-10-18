package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IngredientNameConstraintRepositoryIT {

    @Autowired
    private IngredientNameConstraintRepository repository;

    @Test
    void should_insert_ingredient_name_constraint_when_providing_id() {
        String ingredientId = generateId();
        IngredientNameConstraintEntity expectedEntity = new IngredientNameConstraintEntity(ingredientId, "ingredientName");

        repository.save(new IngredientNameConstraintEntity(expectedEntity.getIngredientId(), expectedEntity.getIngredientName()));

        Optional<IngredientNameConstraintEntity> actualEntity = repository.findById(ingredientId);
        Assertions.assertThat(actualEntity).contains(expectedEntity);
    }

    @Test
    void should_return_name_exists() {
        String ingredientId = generateId();
        String ingredientName = "ingredientName";
        IngredientNameConstraintEntity entity = new IngredientNameConstraintEntity(ingredientId, ingredientName);
        repository.save(entity);

        boolean nameExists = repository.nameExists(ingredientName);

        assertThat(nameExists).isTrue();
    }

    @Test
    void should_return_name_does_not_exist() {
        boolean nameExists = repository.nameExists("ingredientName");

        assertThat(nameExists).isFalse();
    }


    @Test
    void should_return_name_exists_for_another_ingredient() {
        String anotherIngredientId = generateId();
        String ingredientName = "ingredientName";
        IngredientNameConstraintEntity entity = new IngredientNameConstraintEntity(anotherIngredientId, ingredientName);
        repository.save(entity);
        String ingredientId = generateId();

        boolean nameExistsForAnotherIngredient = repository.nameExistsForAnotherIngredient(ingredientId, ingredientName);

        assertThat(nameExistsForAnotherIngredient).isTrue();
    }

    @Test
    void should_return_name_does_not_exist_for_another_ingredient_when_name_does_not_exist() {
        boolean nameExistsForAnotherIngredient = repository.nameExistsForAnotherIngredient(generateId(), "ingredientName");

        assertThat(nameExistsForAnotherIngredient).isFalse();
    }

    @Test
    void should_return_name_does_not_exist_for_another_ingredient_when_name_exists_for_same_ingredient() {
        String ingredientId = generateId();
        String ingredientName = "ingredientName";
        IngredientNameConstraintEntity entity = new IngredientNameConstraintEntity(ingredientId, ingredientName);
        repository.save(entity);

        boolean nameExistsForAnotherIngredient = repository.nameExistsForAnotherIngredient(ingredientId, ingredientName);

        assertThat(nameExistsForAnotherIngredient).isFalse();
    }

    @Test
    void should_retrieve_ingredient_by_name_when_ingredient_exists() {
        String ingredientName = "ingredientName";
        IngredientNameConstraintEntity expectedEntity = new IngredientNameConstraintEntity(generateId(), ingredientName);
        repository.save(expectedEntity);

        Optional<IngredientNameConstraintEntity> ingredient = repository.findByIngredientName(ingredientName);

        Assertions.assertThat(ingredient).contains(expectedEntity);
    }

    @Test
    void should_not_retrieve_ingredient_by_name_when_ingredient_does_not_exist() {
        Optional<IngredientNameConstraintEntity> ingredient = repository.findByIngredientName("ingredientName");

        Assertions.assertThat(ingredient).isEmpty();
    }
}