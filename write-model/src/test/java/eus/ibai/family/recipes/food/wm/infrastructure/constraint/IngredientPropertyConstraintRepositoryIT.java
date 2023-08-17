package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class IngredientPropertyConstraintRepositoryIT {

    @Autowired
    private IngredientNameConstraintRepository ingredientRepository;

    @Autowired
    private PropertyNameConstraintRepository propertyRepository;

    @Autowired
    private IngredientPropertyConstraintRepository ingredientPropertyRepository;

    @Test
    void should_return_ingredient_property_exists_by_property_id() {
        IngredientNameConstraintEntity ingredientEntity = new IngredientNameConstraintEntity("ingredientId", "ingredientName");
        ingredientRepository.save(ingredientEntity);
        PropertyNameConstraintEntity propertyEntity = new PropertyNameConstraintEntity("propertyId", "propertyName");
        propertyRepository.save(propertyEntity);
        IngredientPropertyConstraintEntity ingredientPropertyEntity = new IngredientPropertyConstraintEntity("ingredientId", "propertyId");
        ingredientPropertyRepository.save(ingredientPropertyEntity);

        boolean propertyExists = ingredientPropertyRepository.propertyExists("propertyId");

        assertThat(propertyExists).isTrue();
    }

    @Test
    void should_return_ingredient_property_does_not_exist_by_property_id() {
        boolean propertyExists = ingredientPropertyRepository.propertyExists("propertyId");

        assertThat(propertyExists).isFalse();
    }
}