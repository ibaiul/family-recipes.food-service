package eus.ibai.family.recipes.food.rm;

import eus.ibai.family.recipes.food.rm.application.dto.*;
import eus.ibai.family.recipes.food.rm.test.AcceptanceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static eus.ibai.family.recipes.food.test.TestUtils.authenticate;
import static eus.ibai.family.recipes.food.test.TestUtils.fixedTime;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.assertj.core.api.Assertions.assertThat;

class UserJourneyTest extends AcceptanceTest {

    @Autowired
    private ApplicationContext applicationContext;

    private WebTestClient webTestClient;

    private String bearerToken;

    private Map<String, BasicRecipeDto> recipes;

    private Map<String, BasicIngredientDto> ingredients;

    private Map<String, BasicPropertyDto> properties;

    @BeforeEach
    void beforeEach() {
        recipes = new HashMap<>();
        ingredients = new HashMap<>();
        properties = new HashMap<>();
        createTestData();
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();
        bearerToken = authenticate(webTestClient).accessToken();
    }

    @Test
    void as_a_user_I_can_query_recipes() {
        getAllRecipes();
        getRecipeById();
        getRecipesByIngredientId();
        getRecipesByPropertyId();
    }

    @Test
    void as_a_user_I_can_query_ingredients() {
        getAllIngredients();
        getIngredientById();
        getIngredientsByPropertyId();
    }

    @Test
    void as_a_user_I_can_query_properties() {
        getAllProperties();
        getPropertyById();
    }

    private void getAllRecipes() {
        List<BasicRecipeDto> queryResult = webTestClient.get()
                .uri("/recipes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BasicRecipeDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(queryResult).containsOnly(recipes.values().toArray(new BasicRecipeDto[0]));
    }

    private void getRecipeById() {
        BasicRecipeDto recipe = recipes.get("Pasta carbonara");
        BasicIngredientDto ingredient = ingredients.get("Spaghetti");
        RecipeDto expectedRecipe = new RecipeDto(recipe.id(), recipe.name(), Set.of("https://pasta.com"), Set.of(new RecipeIngredientDto(ingredient.id(), ingredient.name(), fixedTime())));

        webTestClient.get()
                .uri("/recipes/" + recipes.get("Pasta carbonara").id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(RecipeDto.class).isEqualTo(expectedRecipe);
    }

    private void getRecipesByIngredientId() {
        List<BasicRecipeDto> queryResult = webTestClient.get()
                .uri("/recipes?ingredientId=" + ingredients.get("Rice").id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BasicRecipeDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(queryResult).containsOnly(recipes.get("Paella"));
    }

    private void getRecipesByPropertyId() {
        List<BasicRecipeDto> queryResult = webTestClient.get()
                .uri("/recipes?propertyId=" + properties.get("Carbohydrates").id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BasicRecipeDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(queryResult).containsOnly(recipes.get("Pasta carbonara"), recipes.get("Paella"));
    }

    private void getAllIngredients() {
        List<BasicIngredientDto> queryResult = webTestClient.get()
                .uri("/ingredients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BasicIngredientDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(queryResult).containsOnly(ingredients.values().toArray(new BasicIngredientDto[0]));
    }

    private void getIngredientById() {
        BasicIngredientDto ingredient = ingredients.get("Spaghetti");
        BasicPropertyDto property = properties.get("Carbohydrates");
        IngredientDto expectedIngredient = new IngredientDto(ingredient.id(), ingredient.name(), Set.of(new IngredientPropertyDto(property.id(), property.name(), fixedTime())));

        webTestClient.get()
                .uri("/ingredients/" + ingredients.get("Spaghetti").id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(IngredientDto.class).isEqualTo(expectedIngredient);
    }

    private void getIngredientsByPropertyId() {
        List<BasicIngredientDto> queryResult = webTestClient.get()
                .uri("/ingredients?propertyId=" + properties.get("Carbohydrates").id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BasicIngredientDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(queryResult).containsOnly(ingredients.get("Spaghetti"), ingredients.get("Rice"));
    }

    private void getAllProperties() {
        List<BasicPropertyDto> queryResult = webTestClient.get()
                .uri("/properties")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BasicPropertyDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(queryResult).containsOnly(properties.values().toArray(new BasicPropertyDto[0]));
    }

    private void getPropertyById() {
        BasicPropertyDto expectedProperty = properties.get("Fiber");

        webTestClient.get()
                .uri("/properties/" + properties.get("Fiber").id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(BasicPropertyDto.class).isEqualTo(expectedProperty);
    }

    private void createTestData() {
        BasicRecipeDto recipeDto = new BasicRecipeDto(generateId(), "Pasta carbonara");
        recipeEntityRepository.saveNew(recipeDto.id(), recipeDto.name(), new String[]{"https://pasta.com"}).block();
        BasicIngredientDto ingredientDto = new BasicIngredientDto(generateId(), "Spaghetti");
        ingredientEntityRepository.saveNew(ingredientDto.id(), ingredientDto.name()).block();
        recipeIngredientEntityRepository.saveNew(recipeDto.id(), ingredientDto.id(), fixedTime()).block();
        BasicPropertyDto propertyDto = new BasicPropertyDto(generateId(), "Carbohydrates");
        propertyEntityRepository.saveNew(propertyDto.id(), propertyDto.name()).block();
        ingredientPropertyEntityRepository.saveNew(ingredientDto.id(), propertyDto.id(), fixedTime()).block();

        BasicRecipeDto recipeDto2 = new BasicRecipeDto(generateId(), "Paella");
        recipeEntityRepository.saveNew(recipeDto2.id(), recipeDto2.name(), new String[]{"https://paella.com"}).block();
        BasicIngredientDto ingredientDto2 = new BasicIngredientDto(generateId(), "Rice");
        ingredientEntityRepository.saveNew(ingredientDto2.id(), ingredientDto2.name()).block();
        recipeIngredientEntityRepository.saveNew(recipeDto2.id(), ingredientDto2.id(), fixedTime()).block();
        ingredientPropertyEntityRepository.saveNew(ingredientDto2.id(), propertyDto.id(), fixedTime()).block();

        BasicRecipeDto recipeDto3 = new BasicRecipeDto(generateId(), "Mix salad");
        recipeEntityRepository.saveNew(recipeDto3.id(), recipeDto3.name(), new String[0]).block();
        BasicIngredientDto ingredientDto3 = new BasicIngredientDto(generateId(), "Orange");
        ingredientEntityRepository.saveNew(ingredientDto3.id(), ingredientDto3.name()).block();
        BasicPropertyDto propertyDto2 = new BasicPropertyDto(generateId(), "Fiber");
        propertyEntityRepository.saveNew(propertyDto2.id(), propertyDto2.name()).block();

        recipes.put(recipeDto.name(), recipeDto);
        recipes.put(recipeDto2.name(), recipeDto2);
        recipes.put(recipeDto3.name(), recipeDto3);
        ingredients.put(ingredientDto.name(), ingredientDto);
        ingredients.put(ingredientDto2.name(), ingredientDto2);
        ingredients.put(ingredientDto3.name(), ingredientDto3);
        properties.put(propertyDto.name(), propertyDto);
        properties.put(propertyDto2.name(), propertyDto2);
    }
}
