package eus.ibai.family.recipes.food.rm.application.controller;

import eus.ibai.family.recipes.food.exception.RecipeNotFoundException;
import eus.ibai.family.recipes.food.file.StorageFile;
import eus.ibai.family.recipes.food.rm.domain.file.FileStorage;
import eus.ibai.family.recipes.food.rm.domain.recipe.*;
import eus.ibai.family.recipes.food.rm.infrastructure.config.RecipeProperties;
import eus.ibai.family.recipes.food.rm.infrastructure.config.SecurityConfig;
import eus.ibai.family.recipes.food.security.*;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.http.HttpStatusCode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static eus.ibai.family.recipes.food.test.TestUtils.authenticate;
import static eus.ibai.family.recipes.food.test.TestUtils.fixedTime;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;

@WebFluxTest(controllers = {RecipeController.class, AuthController.class})
@Import({GlobalSecurityConfig.class, SecurityConfig.class, JwtService.class, JwtProperties.class, RecipeProperties.class, UserProperties.class, ServiceProperties.class})
class RecipeControllerIT {

    @MockBean
    private ReactorQueryGateway queryGateway;

    @MockBean
    private FileStorage fileStorage;
    
    @Autowired
    private WebTestClient webTestClient;

    private String bearerToken;

    @BeforeEach
    void beforeEach() {
        bearerToken = authenticate(webTestClient).accessToken();
    }

    @Test
    void should_retrieve_recipe() {
        Set<String> links = Set.of("https://lentils.com", "https://chorizo.com");
        Set<String> tags = Set.of("First course", "Spanish cuisine");
        Set<String> images = Set.of(generateId(), generateId());
        RecipeProjection recipe = new RecipeProjection(generateId(), "Lentils with chorizo", links,
                Set.of(new RecipeIngredientProjection("ingredientId", "Lentils", fixedTime())), tags, images);
        when(queryGateway.streamingQuery(new FindRecipeByIdQuery(recipe.id()), RecipeProjection.class)).thenReturn(Flux.just(recipe));

        webTestClient.get()
                .uri("/recipes/" + recipe.id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(recipe.id())
                .jsonPath("$.name").isEqualTo("Lentils with chorizo")
                .jsonPath("$.ingredients[0].id").isEqualTo("ingredientId")
                .jsonPath("$.ingredients[0].name").isEqualTo("Lentils")
                .jsonPath("$.ingredients[0].addedOn").isEqualTo("1970-01-01T00:00:00.000")
                .jsonPath("$.links").value(containsInAnyOrder(links.toArray()))
                .jsonPath("$.tags").value(containsInAnyOrder(tags.toArray()))
                .jsonPath("$.images").value(containsInAnyOrder(images.toArray()));
    }

    @Test
    void should_serialize_recipe_ingredient_date_with_three_digit_millis() {
        String expectedDateTimeFormat = "2023-04-14T22:39:00.200";
        RecipeProjection recipe = new RecipeProjection(generateId(), "Lentils with chorizo", Set.of("https://lentils.com"),
                Set.of(new RecipeIngredientProjection(generateId(), "Lentils", LocalDateTime.parse(expectedDateTimeFormat))), Set.of("First course"), emptySet());
        when(queryGateway.streamingQuery(new FindRecipeByIdQuery(recipe.id()), RecipeProjection.class)).thenReturn(Flux.just(recipe));

        webTestClient.get()
                .uri("/recipes/" + recipe.id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.ingredients[0].addedOn").isEqualTo(expectedDateTimeFormat);
    }

    @Test
    void should_not_retrieve_recipe_that_does_not_exist() {
        String recipeId = generateId();
        when(queryGateway.streamingQuery(new FindRecipeByIdQuery(recipeId), RecipeProjection.class)).thenReturn(Flux.error(new RecipeNotFoundException("")));

        webTestClient.get()
                .uri("/recipes/" + recipeId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_not_retrieve_recipe_when_recipe_id_invalid() {
        webTestClient.get()
                .uri("/recipes/recipeId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void should_retrieve_all_recipes() {
        Set<RecipeIngredientProjection> ingredients = Set.of(new RecipeIngredientProjection(generateId(), "Beans", fixedTime()));
        List<RecipeProjection> recipes = List.of(
                new RecipeProjection(generateId(), "Black beans", Set.of("https://black.beans.com"), ingredients, Set.of("First course"), Set.of(generateId())),
                new RecipeProjection(generateId(), "Green beans", Set.of("https://green.beans.com"), ingredients, Set.of("First course"), Set.of(generateId())));
        when(queryGateway.streamingQuery(new FindRecipesByQuery(null, null, null), RecipeProjection.class)).thenReturn(Flux.fromIterable(recipes));

        webTestClient.get()
                .uri("/recipes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(recipes.get(0).id())
                .jsonPath("$[0].name").isEqualTo(recipes.get(0).name())
                .jsonPath("$[0].ingredients").doesNotHaveJsonPath()
                .jsonPath("$[0].links").doesNotHaveJsonPath()
                .jsonPath("$[0].tags").doesNotHaveJsonPath()
                .jsonPath("$[1].id").isEqualTo(recipes.get(1).id())
                .jsonPath("$[1].name").isEqualTo(recipes.get(1).name())
                .jsonPath("$[1].ingredients").doesNotHaveJsonPath()
                .jsonPath("$[1].links").doesNotHaveJsonPath()
                .jsonPath("$[1].tags").doesNotHaveJsonPath()
                .jsonPath("$[2]").doesNotHaveJsonPath();
    }

    @Test
    void should_retrieve_recipes_by_ingredient_id() {
        String ingredientId = generateId();
        Set<RecipeIngredientProjection> ingredients = Set.of(new RecipeIngredientProjection(ingredientId, "Beans", fixedTime()));
        List<RecipeProjection> recipes = List.of(
                new RecipeProjection(generateId(), "Black beans", Set.of("https://black.beans.com"), ingredients, Set.of("First course"), Set.of(generateId())),
                new RecipeProjection(generateId(), "Green beans", Set.of("https://green.beans.com"), ingredients, Set.of("First course"), Set.of(generateId())));
        when(queryGateway.streamingQuery(new FindRecipesByQuery(ingredientId, null, null), RecipeProjection.class)).thenReturn(Flux.fromIterable(recipes));

        webTestClient.get()
                .uri("/recipes?ingredientId=" + ingredientId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(recipes.get(0).id())
                .jsonPath("$[0].name").isEqualTo(recipes.get(0).name())
                .jsonPath("$[0].ingredients").doesNotHaveJsonPath()
                .jsonPath("$[0].links").doesNotHaveJsonPath()
                .jsonPath("$[0].tags").doesNotHaveJsonPath()
                .jsonPath("$[1].id").isEqualTo(recipes.get(1).id())
                .jsonPath("$[1].name").isEqualTo(recipes.get(1).name())
                .jsonPath("$[1].ingredients").doesNotHaveJsonPath()
                .jsonPath("$[1].links").doesNotHaveJsonPath()
                .jsonPath("$[1].tags").doesNotHaveJsonPath()
                .jsonPath("$[2]").doesNotHaveJsonPath();
    }

    @Test
    void should_retrieve_recipes_by_property_id() {
        Set<RecipeIngredientProjection> ingredients = Set.of(new RecipeIngredientProjection(generateId(), "Beans", fixedTime()));
        List<RecipeProjection> recipes = List.of(
                new RecipeProjection(generateId(), "Black beans", Set.of("https://black.beans.com"), ingredients, Set.of("First course"), Set.of(generateId())),
                new RecipeProjection(generateId(), "Green beans", Set.of("https://green.beans.com"), ingredients, Set.of("First course"), Set.of(generateId())));
        String propertyId = generateId();
        when(queryGateway.streamingQuery(new FindRecipesByQuery(null, propertyId, null), RecipeProjection.class)).thenReturn(Flux.fromIterable(recipes));

        webTestClient.get()
                .uri("/recipes?propertyId=" + propertyId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(recipes.get(0).id())
                .jsonPath("$[0].name").isEqualTo(recipes.get(0).name())
                .jsonPath("$[0].ingredients").doesNotHaveJsonPath()
                .jsonPath("$[0].links").doesNotHaveJsonPath()
                .jsonPath("$[0].tags").doesNotHaveJsonPath()
                .jsonPath("$[1].id").isEqualTo(recipes.get(1).id())
                .jsonPath("$[1].name").isEqualTo(recipes.get(1).name())
                .jsonPath("$[1].ingredients").doesNotHaveJsonPath()
                .jsonPath("$[1].links").doesNotHaveJsonPath()
                .jsonPath("$[1].tags").doesNotHaveJsonPath()
                .jsonPath("$[2]").doesNotHaveJsonPath();
    }

    @Test
    void should_retrieve_recipes_by_tag() {
        Set<RecipeIngredientProjection> ingredients = Set.of(new RecipeIngredientProjection("ingredientId", "Beans", fixedTime()));
        List<RecipeProjection> recipes = List.of(
                new RecipeProjection(generateId(), "Black beans", Set.of("https://black.beans.com"), ingredients, Set.of("First course"), Set.of(generateId())),
                new RecipeProjection(generateId(), "Green beans", Set.of("https://green.beans.com"), ingredients, Set.of("First course"), Set.of(generateId())));
        when(queryGateway.streamingQuery(new FindRecipesByQuery(null, null, "First course"), RecipeProjection.class)).thenReturn(Flux.fromIterable(recipes));

        webTestClient.get()
                .uri("/recipes?tag=First course")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(recipes.get(0).id())
                .jsonPath("$[0].name").isEqualTo(recipes.get(0).name())
                .jsonPath("$[0].ingredients").doesNotHaveJsonPath()
                .jsonPath("$[0].links").doesNotHaveJsonPath()
                .jsonPath("$[0].tags").doesNotHaveJsonPath()
                .jsonPath("$[1].id").isEqualTo(recipes.get(1).id())
                .jsonPath("$[1].name").isEqualTo(recipes.get(1).name())
                .jsonPath("$[1].ingredients").doesNotHaveJsonPath()
                .jsonPath("$[1].links").doesNotHaveJsonPath()
                .jsonPath("$[1].tags").doesNotHaveJsonPath()
                .jsonPath("$[2]").doesNotHaveJsonPath();
    }

    @ParameterizedTest
    @MethodSource
    void should_not_retrieve_recipes_when_query_filters_invalid(String queryParams) {
        webTestClient.get()
                .uri("/recipes?" + queryParams)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isBadRequest();
    }

    private static Stream<String> should_not_retrieve_recipes_when_query_filters_invalid() {
        return Stream.of("ingredientId=ch>", "propertyId=1111");
    }

    @Test
    void should_retrieve_all_recipe_tags() {
        List<String> recipeTags = List.of("First course", "Italian cuisine");
        when(queryGateway.streamingQuery(new FindRecipeTagsQuery(), String.class)).thenReturn(Flux.fromIterable(recipeTags));

        webTestClient.get()
                .uri("/recipes/tags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$[0].name").isEqualTo(recipeTags.get(0))
                .jsonPath("$[1].name").isEqualTo(recipeTags.get(1))
                .jsonPath("$[2]").doesNotHaveJsonPath();
    }

    @Test
    void should_download_recipe_image() {
        Set<String> links = Set.of("https://lentils.com", "https://chorizo.com");
        Set<String> tags = Set.of("First course", "Spanish cuisine");
        String imageId = generateId();
        Set<String> images = Set.of(imageId, generateId());
        RecipeProjection recipe = new RecipeProjection(generateId(), "Lentils with chorizo", links,
                Set.of(new RecipeIngredientProjection("ingredientId", "Lentils", fixedTime())), tags, images);
        when(queryGateway.streamingQuery(new FindRecipeByIdQuery(recipe.id()), RecipeProjection.class)).thenReturn(Flux.just(recipe));
        byte[] fileContent = "foo".getBytes();
        when(fileStorage.retrieveFile("recipes/images/" + imageId)).thenReturn(Mono.just(new StorageFile(imageId, IMAGE_PNG_VALUE, 1, ByteBuffer.wrap(fileContent), Collections.emptyMap())));

        webTestClient.get()
                .uri("/recipes/%s/images/%s".formatted(recipe.id(), imageId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG)
                .expectHeader().contentLength(fileContent.length)
                .expectBody(ByteBuffer.class);
    }

    @Test
    void should_not_download_recipe_image_when_recipe_does_not_exist() {
        String recipeId = generateId();
        when(queryGateway.streamingQuery(new FindRecipeByIdQuery(recipeId), RecipeProjection.class)).thenReturn(Flux.error(new RecipeNotFoundException("")));

        webTestClient.get()
                .uri("/recipes/%s/image/%s".formatted(recipeId, generateId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_not_download_recipe_image_when_does_not_exist() {
        Set<String> links = Set.of("https://lentils.com", "https://chorizo.com");
        Set<String> tags = Set.of("First course", "Spanish cuisine");
        Set<String> images = Set.of(generateId(), generateId());
        RecipeProjection recipe = new RecipeProjection(generateId(), "Lentils with chorizo", links,
                Set.of(new RecipeIngredientProjection("ingredientId", "Lentils", fixedTime())), tags, images);
        when(queryGateway.streamingQuery(new FindRecipeByIdQuery(recipe.id()), RecipeProjection.class)).thenReturn(Flux.just(recipe));

        webTestClient.get()
                .uri("/recipes/%s/images/%s".formatted(recipe.id(), generateId()))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_not_download_recipe_image_when_not_found_in_storage() {
        Set<String> links = Set.of("https://lentils.com", "https://chorizo.com");
        Set<String> tags = Set.of("First course", "Spanish cuisine");
        String imageId = generateId();
        Set<String> images = Set.of(imageId, generateId());
        RecipeProjection recipe = new RecipeProjection(generateId(), "Lentils with chorizo", links,
                Set.of(new RecipeIngredientProjection("ingredientId", "Lentils", fixedTime())), tags, images);
        when(queryGateway.streamingQuery(new FindRecipeByIdQuery(recipe.id()), RecipeProjection.class)).thenReturn(Flux.just(recipe));
        when(fileStorage.retrieveFile("recipes/images/" + imageId)).thenReturn(Mono.error(new FileNotFoundException()));

        webTestClient.get()
                .uri("/recipes/%s/images/%s".formatted(recipe.id(), imageId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_not_download_recipe_image_when_cannot_retrieve_from_storage() {
        Set<String> links = Set.of("https://lentils.com", "https://chorizo.com");
        Set<String> tags = Set.of("First course", "Spanish cuisine");
        String imageId = generateId();
        Set<String> images = Set.of(imageId, generateId());
        RecipeProjection recipe = new RecipeProjection(generateId(), "Lentils with chorizo", links,
                Set.of(new RecipeIngredientProjection("ingredientId", "Lentils", fixedTime())), tags, images);
        when(queryGateway.streamingQuery(new FindRecipeByIdQuery(recipe.id()), RecipeProjection.class)).thenReturn(Flux.just(recipe));
        when(fileStorage.retrieveFile("recipes/images/" + imageId)).thenReturn(Mono.error(new IOException()));

        webTestClient.get()
                .uri("/recipes/%s/images/%s".formatted(recipe.id(), imageId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR);
    }
}
