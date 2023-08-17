package eus.ibai.family.recipes.food.rm.domain.ingredient;

import eus.ibai.family.recipes.food.exception.IngredientNotFoundException;
import eus.ibai.family.recipes.food.rm.infrastructure.model.IngredientEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.model.PropertyEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.IngredientEntityRepository;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.IngredientPropertyEntityRepository;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.PropertyEntityRepository;
import eus.ibai.family.recipes.food.rm.test.DataCleanupExtension;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.util.Set;

import static eus.ibai.family.recipes.food.test.TestUtils.fixedTime;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(DataCleanupExtension.class)
class IngredientProjectionQueryIT {

    @Autowired
    private ReactorQueryGateway queryGateway;

    @Autowired
    private IngredientEntityRepository ingredientEntityRepository;

    @Autowired
    private PropertyEntityRepository propertyEntityRepository;

    @Autowired
    private IngredientPropertyEntityRepository ingredientPropertyEntityRepository;

    private IngredientProjection ingredientWithProperty;

    private IngredientProjection ingredientWithoutProperty;

    @BeforeEach
    void beforeEach() {
        createTestData();
    }

    @Test
    void should_find_ingredient_by_id() {
        queryGateway.query(new FindIngredientByIdQuery(ingredientWithProperty.id()), IngredientProjection.class)
                .as(StepVerifier::create)
                .expectNext(ingredientWithProperty)
                .verifyComplete();
    }

    @Test
    void should_not_find_ingredient_by_id_if_does_not_exist() {
        queryGateway.query(new FindIngredientByIdQuery(generateId()), IngredientProjection.class)
                .as(StepVerifier::create)
                .verifyError(IngredientNotFoundException.class);
    }

    @Test
    void should_find_all_ingredients() {
        queryGateway.streamingQuery(new FindIngredientsByQuery(null), IngredientProjection.class)
                .as(StepVerifier::create)
                .expectNext(new IngredientProjection[]{
                        new IngredientProjection(ingredientWithProperty.id(), ingredientWithProperty.name()),
                        new IngredientProjection(ingredientWithoutProperty.id(), ingredientWithoutProperty.name())
                })
                .verifyComplete();
    }

    @Test
    void should_find_all_containing_property() {
        queryGateway.streamingQuery(new FindIngredientsByQuery(ingredientWithProperty.properties().iterator().next().id()), IngredientProjection.class)
                .as(StepVerifier::create)
                .expectNext(new IngredientProjection(ingredientWithProperty.id(), ingredientWithProperty.name()))
                .verifyComplete();
    }

    private void createTestData() {
        PropertyEntity propertyEntity = new PropertyEntity(generateId(), "Carbohydrates");
        IngredientEntity ingredientEntity = new IngredientEntity(generateId(), "Spaghetti");
        ingredientWithProperty = propertyEntityRepository.saveNew(propertyEntity.getId(), propertyEntity.getName())
                .then(ingredientEntityRepository.saveNew(ingredientEntity.getId(), ingredientEntity.getName()))
                .then(ingredientPropertyEntityRepository.saveNew(ingredientEntity.getId(), propertyEntity.getId(), fixedTime()))
                .thenReturn(new IngredientProjection(ingredientEntity.getId(), ingredientEntity.getName(),
                        Set.of(new IngredientPropertyProjection(propertyEntity.getId(), propertyEntity.getName(), fixedTime()))))
                .block();

        ingredientEntity = new IngredientEntity(generateId(), "Egg");
        ingredientWithoutProperty = ingredientEntityRepository.saveNew(ingredientEntity.getId(), ingredientEntity.getName())
                .thenReturn(new IngredientProjection(ingredientEntity.getId(), ingredientEntity.getName()))
                .block();
    }
}
