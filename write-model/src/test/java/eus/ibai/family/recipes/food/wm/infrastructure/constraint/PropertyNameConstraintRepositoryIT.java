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
class PropertyNameConstraintRepositoryIT {

    @Autowired
    private PropertyNameConstraintRepository repository;

    @Test
    void should_insert_property_name_constraint_when_providing_id() {
        String propertyId = generateId();
        PropertyNameConstraintEntity expectedEntity = new PropertyNameConstraintEntity(propertyId, "propertyName");

        int insertedAmount = repository.insert(expectedEntity.getPropertyId(), expectedEntity.getPropertyName());

        assertThat(insertedAmount).isEqualTo(1);
        Optional<PropertyNameConstraintEntity> actualEntity = repository.findById(propertyId);
        Assertions.assertThat(actualEntity).contains(expectedEntity);
    }

    @Test
    void should_return_name_exists() {
        String propertyName = "propertyName";
        PropertyNameConstraintEntity entity = new PropertyNameConstraintEntity(generateId(), propertyName);
        repository.save(entity);

        boolean nameExists = repository.nameExists(propertyName);

        assertThat(nameExists).isTrue();
    }

    @Test
    void should_return_name_does_not_exist() {
        boolean nameExists = repository.nameExists("propertyName");

        assertThat(nameExists).isFalse();
    }


    @Test
    void should_return_name_exists_for_another_property() {
        String anotherPropertyId = generateId();
        String propertyName = "propertyName";
        PropertyNameConstraintEntity entity = new PropertyNameConstraintEntity(anotherPropertyId, propertyName);
        repository.save(entity);
        String propertyId = generateId();

        boolean nameExistsForAnotherProperty = repository.nameExistsForAnotherProperty(propertyId, propertyName);

        assertThat(nameExistsForAnotherProperty).isTrue();
    }

    @Test
    void should_return_name_does_not_exist_for_another_property_when_name_does_not_exist() {
        boolean nameExistsForAnotherProperty = repository.nameExistsForAnotherProperty(generateId(), "propertyName");

        assertThat(nameExistsForAnotherProperty).isFalse();
    }

    @Test
    void should_return_name_does_not_exist_for_another_property_when_name_exists_for_same_property() {
        String propertyId = generateId();
        String propertyName = "propertyName";
        PropertyNameConstraintEntity entity = new PropertyNameConstraintEntity(propertyId, propertyName);
        repository.save(entity);

        boolean nameExistsForAnotherProperty = repository.nameExistsForAnotherProperty(propertyId, propertyName);

        assertThat(nameExistsForAnotherProperty).isFalse();
    }

    @Test
    void should_retrieve_property_by_name_when_property_exists() {
        String propertyName = "propertyName";
        PropertyNameConstraintEntity expectedEntity = new PropertyNameConstraintEntity(generateId(), propertyName);
        repository.save(expectedEntity);

        Optional<PropertyNameConstraintEntity> property = repository.findByPropertyName(propertyName);

        Assertions.assertThat(property).contains(expectedEntity);
    }

    @Test
    void should_not_retrieve_property_by_name_when_property_does_not_exist() {
        Optional<PropertyNameConstraintEntity> property = repository.findByPropertyName("propertyName");

        Assertions.assertThat(property).isEmpty();
    }
}