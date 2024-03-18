package eus.ibai.family.recipes.food.rm;

import eus.ibai.family.recipes.food.rm.application.dto.*;
import eus.ibai.family.recipes.food.rm.infrastructure.model.RecipeEntity;
import eus.ibai.family.recipes.food.rm.test.AcceptanceTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Stream;

import static eus.ibai.family.recipes.food.test.FileTestUtils.*;
import static eus.ibai.family.recipes.food.test.TestUtils.authenticate;
import static eus.ibai.family.recipes.food.test.TestUtils.fixedTime;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;

class UserJourneyTest extends AcceptanceTest {

    @Autowired
    private ApplicationContext applicationContext;

    private WebTestClient webTestClient;

    private String bearerToken;

    private Map<String, BasicRecipeDto> recipes;

    private Map<String, BasicIngredientDto> ingredients;

    private Map<String, BasicPropertyDto> properties;

    private Set<String> recipeTags;

    private String imageId;

    @BeforeEach
    void beforeEach() {
        createS3Bucket(s3Client);
        imageId = storeRecipeImage(s3Client);
        recipes = new HashMap<>();
        ingredients = new HashMap<>();
        properties = new HashMap<>();
        recipeTags = new HashSet<>();
        createTestData();
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
                .configureClient()
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(config ->  config.defaultCodecs().maxInMemorySize(22000))
                        .build())
                .build();
        bearerToken = authenticate(webTestClient).accessToken();
    }

    @Test
    void as_a_user_I_can_query_recipes() {
        getAllRecipes();
        getRecipeById();
        getRecipesByIngredientId();
        getRecipesByPropertyId();
        getAllRecipeTags();
        getRecipesByTag();
    }

    @Test
    void as_a_user_I_can_download_recipe_images() {
        downloadRecipeImage();
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

        assertThat(queryResult).containsExactlyInAnyOrderElementsOf(recipes.values());
    }

    private void getRecipeById() {
        BasicRecipeDto recipe = recipes.get("Pasta carbonara");
        BasicIngredientDto ingredient = ingredients.get("Spaghetti");
        RecipeDto expectedRecipe = new RecipeDto(recipe.id(), recipe.name(), Set.of("https://pasta.com"),
                Set.of(new RecipeIngredientDto(ingredient.id(), ingredient.name(), fixedTime())), Set.of("Italian cuisine", "First course"), Set.of(imageId));

        webTestClient.get()
                .uri("/recipes/" + recipe.id())
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

        assertThat(queryResult).containsExactlyInAnyOrder(recipes.get("Paella"));
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

        assertThat(queryResult).containsExactlyInAnyOrder(recipes.get("Pasta carbonara"), recipes.get("Paella"));
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

        assertThat(queryResult).containsExactlyInAnyOrderElementsOf(ingredients.values());
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

        assertThat(queryResult).containsExactlyInAnyOrder(ingredients.get("Spaghetti"), ingredients.get("Rice"));
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

        assertThat(queryResult).containsExactlyInAnyOrderElementsOf(properties.values());
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

    private void getAllRecipeTags() {
        List<BasicRecipeTagDto> expectedTags = Stream.of("First course", "Spanish cuisine", "Italian cuisine")
                .map(BasicRecipeTagDto::new)
                .toList();

        List<BasicRecipeTagDto> queryResult = webTestClient.get()
                .uri("/recipes/tags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BasicRecipeTagDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(queryResult).containsExactlyInAnyOrderElementsOf(expectedTags);
    }

    private void getRecipesByTag() {
        List<BasicRecipeDto> expectedRecipes = List.of(recipes.get("Pasta carbonara"));

        List<BasicRecipeDto> queryResult = webTestClient.get()
                .uri("/recipes?tag=Italian cuisine")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BasicRecipeDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(queryResult).containsExactlyInAnyOrderElementsOf(expectedRecipes);
    }

    @SneakyThrows
    private void downloadRecipeImage() {
        BasicRecipeDto recipe = recipes.get("Pasta carbonara");

        ByteBuffer fileContent = webTestClient.get()
                .uri("/recipes/" + recipe.id() + "/images/" + imageId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Content-Type", IMAGE_PNG_VALUE)
                .expectHeader().valueEquals("Content-Length", 24018)
                .expectBody(ByteBuffer.class).returnResult().getResponseBody();

        verifyDownloadedRecipeImage(fileContent);
    }

    private void createTestData() {
        BasicRecipeDto recipeDto = new BasicRecipeDto(generateId(), "Pasta carbonara");
        recipeEntityRepository.saveNew(recipeDto.id(), recipeDto.name())
                .thenReturn(new RecipeEntity(recipeDto.id(), recipeDto.name(), Set.of("https://pasta.com"))
                        .addTag("First course")
                        .addTag("Italian cuisine")
                        .addImage(imageId))
                .flatMap(recipeEntityRepository::save).block();
        BasicIngredientDto ingredientDto = new BasicIngredientDto(generateId(), "Spaghetti");
        ingredientEntityRepository.saveNew(ingredientDto.id(), ingredientDto.name()).block();
        recipeIngredientEntityRepository.saveNew(recipeDto.id(), ingredientDto.id(), fixedTime()).block();
        BasicPropertyDto propertyDto = new BasicPropertyDto(generateId(), "Carbohydrates");
        propertyEntityRepository.saveNew(propertyDto.id(), propertyDto.name()).block();
        ingredientPropertyEntityRepository.saveNew(ingredientDto.id(), propertyDto.id(), fixedTime()).block();

        BasicRecipeDto recipeDto2 = new BasicRecipeDto(generateId(), "Paella");
        recipeEntityRepository.saveNew(recipeDto2.id(), recipeDto2.name())
                .thenReturn(new RecipeEntity(recipeDto2.id(), recipeDto2.name(), Set.of("https://paella.com"))
                        .addTag("First course")
                        .addTag("Spanish cuisine"))
                .flatMap(recipeEntityRepository::save).block();
        BasicIngredientDto ingredientDto2 = new BasicIngredientDto(generateId(), "Rice");
        ingredientEntityRepository.saveNew(ingredientDto2.id(), ingredientDto2.name()).block();
        recipeIngredientEntityRepository.saveNew(recipeDto2.id(), ingredientDto2.id(), fixedTime()).block();
        ingredientPropertyEntityRepository.saveNew(ingredientDto2.id(), propertyDto.id(), fixedTime()).block();

        BasicRecipeDto recipeDto3 = new BasicRecipeDto(generateId(), "Mix salad");
        recipeEntityRepository.saveNew(recipeDto3.id(), recipeDto3.name()).block();
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
