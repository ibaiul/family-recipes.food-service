package eus.ibai.family.recipes.food.rm.domain.recipe;

import eus.ibai.family.recipes.food.exception.RecipeNotFoundException;
import eus.ibai.family.recipes.food.rm.infrastructure.model.IngredientEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.model.PropertyEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.model.RecipeEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.*;
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
class RecipeProjectionQueryIT {

    @Autowired
    private ReactorQueryGateway queryGateway;

    @Autowired
    private RecipeEntityRepository recipeRepository;

    @Autowired
    private IngredientEntityRepository ingredientRepository;

    @Autowired
    private PropertyEntityRepository propertyRepository;

    @Autowired
    private RecipeIngredientEntityRepository recipeIngredientRepository;

    @Autowired
    private IngredientPropertyEntityRepository ingredientPropertyRepository;

    private RecipeProjection recipeWithIngredient;

    private RecipeProjection recipeWithIngredientAndProperty;

    private String propertyId;

    @BeforeEach
    void beforeEach() {
        createTestData();
    }

    @Test
    void should_find_recipe_by_id() {
        queryGateway.query(new FindRecipeByIdQuery(recipeWithIngredient.id()), RecipeProjection.class)
                .as(StepVerifier::create)
                .expectNext(recipeWithIngredient)
                .verifyComplete();
    }

    @Test
    void should_not_find_recipe_by_id_if_does_not_exist() {
        queryGateway.query(new FindRecipeByIdQuery(generateId()), RecipeProjection.class)
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_find_all_recipes() {
        queryGateway.streamingQuery(new FindRecipesByQuery(null, null), RecipeProjection.class)
                .as(StepVerifier::create)
                .expectNext(new RecipeProjection[]{
                        new RecipeProjection(recipeWithIngredientAndProperty.id(), recipeWithIngredientAndProperty.name(), recipeWithIngredientAndProperty.links()),
                        new RecipeProjection(recipeWithIngredient.id(), recipeWithIngredient.name(), recipeWithIngredient.links())})
                .verifyComplete();
    }

    @Test
    void should_find_all_containing_ingredient() {
        queryGateway.streamingQuery(new FindRecipesByQuery(recipeWithIngredient.ingredients().iterator().next().id(), null), RecipeProjection.class)
                .as(StepVerifier::create)
                .expectNext(new RecipeProjection(recipeWithIngredient.id(), recipeWithIngredient.name(), recipeWithIngredient.links()))
                .verifyComplete();
    }

    @Test
    void should_find_all_containing_property() {
        queryGateway.streamingQuery(new FindRecipesByQuery(null, propertyId), RecipeProjection.class)
                .as(StepVerifier::create)
                .expectNext(new RecipeProjection(recipeWithIngredientAndProperty.id(), recipeWithIngredientAndProperty.name(), recipeWithIngredientAndProperty.links()))
                .verifyComplete();
    }

    private void createTestData() {
        PropertyEntity propertyEntity = new PropertyEntity(generateId(), "Carbohydrates");
        propertyId = propertyEntity.getId();
        IngredientEntity ingredientEntity = new IngredientEntity(generateId(), "Spaghetti");
        RecipeEntity recipeEntity = new RecipeEntity(generateId(), "Pasta carbonara", Set.of("https://pasta.com"));
        recipeWithIngredientAndProperty = recipeRepository.saveNew(recipeEntity.getId(), recipeEntity.getName(), recipeEntity.getLinks().toArray(new String[0]))
                .then(ingredientRepository.saveNew(ingredientEntity.getId(), ingredientEntity.getName()))
                .then(propertyRepository.saveNew(propertyEntity.getId(), propertyEntity.getName()))
                .then(ingredientPropertyRepository.saveNew(ingredientEntity.getId(), propertyEntity.getId(), fixedTime()))
                .then(recipeIngredientRepository.saveNew(recipeEntity.getId(), ingredientEntity.getId(), fixedTime()))
                .thenReturn(new RecipeProjection(recipeEntity.getId(), recipeEntity.getName(), recipeEntity.getLinks(), Set.of(new RecipeIngredientProjection(ingredientEntity.getId(), ingredientEntity.getName(), fixedTime()))))
                .block();

        ingredientEntity = new IngredientEntity(generateId(), "Rice");
        recipeEntity = new RecipeEntity(generateId(), "Vietnamese style rice", Set.of("https://rice.com"));
        recipeWithIngredient = recipeRepository.saveNew(recipeEntity.getId(), recipeEntity.getName(), recipeEntity.getLinks().toArray(new String[0]))
                .then(ingredientRepository.saveNew(ingredientEntity.getId(), ingredientEntity.getName()))
                .then(recipeIngredientRepository.saveNew(recipeEntity.getId(), ingredientEntity.getId(), fixedTime()))
                .thenReturn(new RecipeProjection(recipeEntity.getId(), recipeEntity.getName(), recipeEntity.getLinks(), Set.of(new RecipeIngredientProjection(ingredientEntity.getId(), ingredientEntity.getName(), fixedTime()))))
                .block();
    }
}
