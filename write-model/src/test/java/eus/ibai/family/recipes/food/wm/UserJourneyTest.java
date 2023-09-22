package eus.ibai.family.recipes.food.wm;

import eus.ibai.family.recipes.food.wm.application.dto.*;
import eus.ibai.family.recipes.food.wm.test.AcceptanceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static eus.ibai.family.recipes.food.test.TestUtils.UUID_PATTERN_STRING;
import static eus.ibai.family.recipes.food.test.TestUtils.authenticate;
import static java.lang.String.format;

class UserJourneyTest extends AcceptanceTest {

    private WebTestClient webTestClient;

    private String bearerToken;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void beforeEach() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();
        bearerToken = authenticate(webTestClient).accessToken();
    }

    @Test
    void as_a_user_I_can_manage_recipes() {
        String recipeUrl = createRecipe("Pasta carbonara");
        updateRecipe(recipeUrl, "Spaghetti carbonara", Set.of("https://pasta.com"));
        String recipeIngredientUrl = addRecipeIngredient(recipeUrl, "Spaghetti");
        createIngredient("Egg");
        addRecipeIngredient(recipeUrl, "Egg");
        removeRecipeIngredient(recipeIngredientUrl);
        tagRecipe(recipeUrl, "First course");
        untagRecipe(recipeUrl, "First course");
        deleteRecipe(recipeUrl);
    }

    @Test
    void as_a_user_I_can_manage_ingredients() {
        String ingredientUrl = createIngredient("Rice");
        updateIngredient(ingredientUrl, "Integral rice");
        String ingredientPropertyUrl = addIngredientProperty(ingredientUrl, "Fiber");
        createProperty("Carbohydrates");
        addIngredientProperty(ingredientUrl, "Carbohydrates");
        removeIngredientProperty(ingredientPropertyUrl);

        String recipeUrl = createRecipe("Vietnamese style rice");
        addRecipeIngredient(recipeUrl, "Integral rice");
        cannotDeleteIngredientBoundToRecipe(ingredientUrl);
        deleteRecipe(recipeUrl);

        deleteIngredient(ingredientUrl);
    }

    @Test
    void as_a_user_I_can_manage_properties() {
        String propertyUrl = createProperty("Vit C");
        updateProperty(propertyUrl, "Vitamin C");

        String ingredientUrl = createIngredient("Orange");
        addIngredientProperty(ingredientUrl, "Vitamin C");
        cannotDeletePropertyBoundToIngredient(propertyUrl);
        deleteIngredient(ingredientUrl);

        deleteProperty(propertyUrl);
    }

    private String createRecipe(String recipeName) {
        AtomicReference<String> recipeUrl = new AtomicReference<>();
        CreateRecipeDto dto = new CreateRecipeDto(recipeName);
        webTestClient.post()
                .uri("/recipes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("location", "/recipes/" + UUID_PATTERN_STRING)
                .expectHeader().values("location", headerValues -> {
                   recipeUrl.set(headerValues.get(0));
                })
                .expectBody().isEmpty();
        return recipeUrl.get();
    }

    private void updateRecipe(String recipeUrl, String recipeName, Set<String> links) {
        UpdateRecipeDto dto = new UpdateRecipeDto(recipeName, links);
        webTestClient.put()
                .uri(recipeUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    private String addRecipeIngredient(String recipeUrl, String ingredientName) {
        AtomicReference<String> recipeIngredientUrl = new AtomicReference<>();
        AddRecipeIngredientDto dto = new AddRecipeIngredientDto(ingredientName);
        webTestClient.post()
                .uri(format(recipeUrl + "/ingredients"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("location", format("%s/ingredients/%s", recipeUrl, UUID_PATTERN_STRING))
                .expectHeader().values("location", headerValues -> {
                    recipeIngredientUrl.set(headerValues.get(0));
                })
                .expectBody().isEmpty();
        return recipeIngredientUrl.get();
    }

    private void removeRecipeIngredient(String recipeIngredientUrl) {
        deleteResource(recipeIngredientUrl);
    }

    private void tagRecipe(String recipeUrl, String tag) {
        AddRecipeTagDto dto = new AddRecipeTagDto(tag);
        webTestClient.post()
                .uri(format(recipeUrl + "/tags"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    private void untagRecipe(String recipeUrl, String tag) {
        webTestClient.delete()
                .uri(format(recipeUrl + "/tags?name=" + tag))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    private void deleteRecipe(String recipeUrl) {
        deleteResource(recipeUrl);
    }

    private String createIngredient(String ingredientName) {
        AtomicReference<String> ingredientUrl = new AtomicReference<>();
        CreateIngredientDto dto = new CreateIngredientDto(ingredientName);
        webTestClient.post()
                .uri("/ingredients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("location", "/ingredients/" + UUID_PATTERN_STRING)
                .expectHeader().values("location", headerValues -> {
                    ingredientUrl.set(headerValues.get(0));
                })
                .expectBody().isEmpty();
        return ingredientUrl.get();
    }

    private void updateIngredient(String ingredientUrl, String ingredientName) {
        UpdateIngredientDto dto = new UpdateIngredientDto(ingredientName);
        webTestClient.put()
                .uri(ingredientUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    private String addIngredientProperty(String ingredientUrl, String propertyName) {
        AtomicReference<String> ingredientProperty = new AtomicReference<>();
        AddIngredientPropertyDto dto = new AddIngredientPropertyDto(propertyName);
        webTestClient.post()
                .uri(format(ingredientUrl + "/properties"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("location", format("%s/properties/%s", ingredientUrl, UUID_PATTERN_STRING))
                .expectHeader().values("location", headerValues -> {
                    ingredientProperty.set(headerValues.get(0));
                })
                .expectBody().isEmpty();
        return ingredientProperty.get();
    }

    private void removeIngredientProperty(String ingredientPropertyUrl) {
        deleteResource(ingredientPropertyUrl);
    }

    private void deleteIngredient(String ingredientUrl) {
        deleteResource(ingredientUrl);
    }

    private void cannotDeleteIngredientBoundToRecipe(String ingredientUrl) {
        webTestClient.delete()
                .uri(ingredientUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody().isEmpty();
    }

    private String createProperty(String propertyName) {
        AtomicReference<String> propertyUrl = new AtomicReference<>();
        CreatePropertyDto dto = new CreatePropertyDto(propertyName);
        webTestClient.post()
                .uri("/properties")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().valueMatches("location", "/properties/" + UUID_PATTERN_STRING)
                .expectHeader().values("location", headerValues -> {
                    propertyUrl.set(headerValues.get(0));
                })
                .expectBody().isEmpty();
        return propertyUrl.get();
    }

    private void updateProperty(String propertyUrl, String propertyName) {
        UpdatePropertyDto dto = new UpdatePropertyDto(propertyName);
        webTestClient.put()
                .uri(propertyUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }

    private void deleteProperty(String propertyUrl) {
        deleteResource(propertyUrl);
    }

    private void cannotDeletePropertyBoundToIngredient(String propertyUrl) {
        webTestClient.delete()
                .uri(propertyUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody().isEmpty();
    }

    private void deleteResource(String resourceUrl) {
        webTestClient.delete()
                .uri(resourceUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();
    }
}
