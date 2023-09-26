package eus.ibai.family.recipes.food.rm.domain.recipe;

import eus.ibai.family.recipes.food.exception.RecipeNotFoundException;
import eus.ibai.family.recipes.food.rm.infrastructure.model.IngredientEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.model.PropertyEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.model.RecipeEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.*;
import eus.ibai.family.recipes.food.rm.test.DataCleanupExtension;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.util.Collections;
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

    private RecipeProjection recipe1;

    private RecipeProjection recipe2;

    private RecipeProjection recipe3;

    private String propertyId;

    @BeforeEach
    void beforeEach() {
        createTestData();
    }

    @Test
    void should_find_recipe_by_id() {
        queryGateway.query(new FindRecipeByIdQuery(recipe1.id()), RecipeProjection.class)
                .as(StepVerifier::create)
                .expectNext(recipe1)
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
        queryGateway.streamingQuery(new FindRecipesByQuery(null, null, null), RecipeProjection.class)
                .as(StepVerifier::create)
                .expectNext(new RecipeProjection[]{
                        new RecipeProjection(recipe2.id(), recipe2.name(), recipe2.links(), Collections.emptySet(), recipe2.tags()),
                        new RecipeProjection(recipe1.id(), recipe1.name(), recipe1.links(), Collections.emptySet(), recipe1.tags()),
                        new RecipeProjection(recipe3.id(), recipe3.name(), recipe3.links(), Collections.emptySet(), recipe3.tags()),
                })
                .verifyComplete();
    }

    @Test
    void should_find_all_containing_ingredient() {
        queryGateway.streamingQuery(new FindRecipesByQuery(recipe1.ingredients().iterator().next().id(), null, null), RecipeProjection.class)
                .as(StepVerifier::create)
                .expectNext(new RecipeProjection(recipe1.id(), recipe1.name(), recipe1.links(), Collections.emptySet(), recipe1.tags()))
                .verifyComplete();
    }

    @Test
    void should_find_all_containing_property() {
        queryGateway.streamingQuery(new FindRecipesByQuery(null, propertyId, null), RecipeProjection.class)
                .as(StepVerifier::create)
                .expectNext(new RecipeProjection(recipe2.id(), recipe2.name(), recipe2.links(), Collections.emptySet(), recipe2.tags()))
                .verifyComplete();
    }

    @Test
    @Disabled("H2 does not support UNNEST as a function unless used as a collection derived table e.g. SELECT * FROM UNNEST(SELECT)")
    void should_find_all_recipe_tags() {
        queryGateway.streamingQuery(new FindRecipeTagsQuery(), String.class)
                .sort()
                .as(StepVerifier::create)
                .expectNext("First course")
                .expectNext("Main course")
                .verifyComplete();
    }

    @Test
    @Disabled("H2 does not support deeply nested references e.g. ANY(r.tags) or UNNEST(r.tags)")
    void should_find_all_containing_tag() {
        queryGateway.streamingQuery(new FindRecipesByQuery(null, null, "Main course"), RecipeProjection.class)
                .as(StepVerifier::create)
                .expectNext(new RecipeProjection(recipe3.id(), recipe3.name(), recipe3.links(), Collections.emptySet(), recipe3.tags()))
                .verifyComplete();
    }

    private void createTestData() {
        PropertyEntity propertyEntity = new PropertyEntity(generateId(), "Carbohydrates");
        propertyId = propertyEntity.getId();
        IngredientEntity ingredientEntity = new IngredientEntity(generateId(), "Spaghetti");
        RecipeEntity recipeEntity = new RecipeEntity(generateId(), "Pasta carbonara", Set.of("https://pasta.com"))
                .addTag("First course");
        recipe2 = recipeRepository.saveNew(recipeEntity.getId(), recipeEntity.getName())
                .then(recipeRepository.save(recipeEntity))
                .then(ingredientRepository.saveNew(ingredientEntity.getId(), ingredientEntity.getName()))
                .then(propertyRepository.saveNew(propertyEntity.getId(), propertyEntity.getName()))
                .then(ingredientPropertyRepository.saveNew(ingredientEntity.getId(), propertyEntity.getId(), fixedTime()))
                .then(recipeIngredientRepository.saveNew(recipeEntity.getId(), ingredientEntity.getId(), fixedTime()))
                .thenReturn(new RecipeProjection(recipeEntity.getId(), recipeEntity.getName(), recipeEntity.getLinks(),
                        Set.of(new RecipeIngredientProjection(ingredientEntity.getId(), ingredientEntity.getName(), fixedTime())), recipeEntity.getTags()))
                .block();

        ingredientEntity = new IngredientEntity(generateId(), "Rice");
        recipeEntity = new RecipeEntity(generateId(), "Vietnamese style rice", Set.of("https://rice.com"))
                .addTag("First course");
        recipe1 = recipeRepository.saveNew(recipeEntity.getId(), recipeEntity.getName())
                .then(recipeRepository.save(recipeEntity))
                .then(ingredientRepository.saveNew(ingredientEntity.getId(), ingredientEntity.getName()))
                .then(recipeIngredientRepository.saveNew(recipeEntity.getId(), ingredientEntity.getId(), fixedTime()))
                .thenReturn(new RecipeProjection(recipeEntity.getId(), recipeEntity.getName(), recipeEntity.getLinks(),
                        Set.of(new RecipeIngredientProjection(ingredientEntity.getId(), ingredientEntity.getName(), fixedTime())), recipeEntity.getTags()))
                .block();

        recipeEntity = new RecipeEntity(generateId(), "Pil-pil style cod", Set.of("https://cod.com"))
                .addTag("Main course");
        recipe3 = recipeRepository.saveNew(recipeEntity.getId(), recipeEntity.getName())
                .then(recipeRepository.save(recipeEntity))
                .thenReturn(new RecipeProjection(recipeEntity.getId(), recipeEntity.getName(), recipeEntity.getLinks(),
                        Set.of(new RecipeIngredientProjection(ingredientEntity.getId(), ingredientEntity.getName(), fixedTime())), recipeEntity.getTags()))
                .block();
    }
}
