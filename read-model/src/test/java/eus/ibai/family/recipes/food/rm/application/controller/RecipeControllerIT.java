package eus.ibai.family.recipes.food.rm.application.controller;

import eus.ibai.family.recipes.food.exception.RecipeNotFoundException;
import eus.ibai.family.recipes.food.rm.application.dto.BasicRecipeDto;
import eus.ibai.family.recipes.food.rm.application.dto.RecipeDto;
import eus.ibai.family.recipes.food.rm.domain.recipe.FindRecipeByIdQuery;
import eus.ibai.family.recipes.food.rm.domain.recipe.FindRecipesByQuery;
import eus.ibai.family.recipes.food.rm.domain.recipe.RecipeIngredientProjection;
import eus.ibai.family.recipes.food.rm.domain.recipe.RecipeProjection;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eus.ibai.family.recipes.food.test.TestUtils.authenticate;
import static eus.ibai.family.recipes.food.test.TestUtils.fixedTime;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {RecipeController.class, AuthController.class})
@Import({SecurityConfig.class, JwtService.class, JwtProperties.class, UserProperties.class})
class RecipeControllerIT {

    @MockBean
    private ReactorQueryGateway queryGateway;
    
    @Autowired
    private WebTestClient webTestClient;

    private String bearerToken;

    @BeforeEach
    void beforeEach() {
        bearerToken = authenticate(webTestClient).accessToken();
    }

    @Test
    void should_retrieve_recipe() {
        RecipeProjection recipe = new RecipeProjection(generateId(), "Lentils", Set.of("https://lentils.com"), Set.of(new RecipeIngredientProjection("recipeId", "Legume", fixedTime())));
        when(queryGateway.streamingQuery(new FindRecipeByIdQuery(recipe.id()), RecipeProjection.class)).thenReturn(Flux.just(recipe));
        RecipeDto expectedRecipeDto = RecipeDto.fromProjection(recipe);

        webTestClient.get()
                .uri("/recipes/" + recipe.id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(RecipeDto.class).isEqualTo(expectedRecipeDto);
    }

    @Test
    void should_serialize_recipe_ingredient_date_with_three_digit_millis() {
        String expectedDateTimeFormat = "2023-04-14T22:39:00.200";
        RecipeProjection recipe = new RecipeProjection(generateId(), "Lentils", Collections.emptySet(), Set.of(new RecipeIngredientProjection("ingredientId", "Legume", LocalDateTime.parse(expectedDateTimeFormat))));
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
        Set<RecipeProjection> recipes = Set.of(
                new RecipeProjection(generateId(), "Black beans", Set.of("https://black.beans.com")),
                new RecipeProjection(generateId(), "Green beans", Set.of("https://green.beans.com")),
                new RecipeProjection(generateId(), "White beans", Set.of("https://white.beans.com")));
        when(queryGateway.streamingQuery(new FindRecipesByQuery(null, null), RecipeProjection.class)).thenReturn(Flux.fromIterable(recipes));
        List<BasicRecipeDto> expectedRecipeDtos = recipes.stream()
                .map(BasicRecipeDto::fromProjection)
                .collect(Collectors.toList());

        webTestClient.get()
                .uri("/recipes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(BasicRecipeDto.class).isEqualTo(expectedRecipeDtos);
    }

    @Test
    void should_retrieve_recipes_by_ingredient_id() {
        String ingredientId = generateId();
        Set<RecipeProjection> recipes = Set.of(
                new RecipeProjection(generateId(), "Black beans", Set.of("https://black.beans.com")),
                new RecipeProjection(generateId(), "Green beans", Set.of("https://green.beans.com")),
                new RecipeProjection(generateId(), "White beans", Set.of("https://white.beans.com")));
        when(queryGateway.streamingQuery(new FindRecipesByQuery(ingredientId, null), RecipeProjection.class)).thenReturn(Flux.fromIterable(recipes));
        List<BasicRecipeDto> expectedRecipeDtos = recipes.stream()
                .map(BasicRecipeDto::fromProjection)
                .collect(Collectors.toList());

        webTestClient.get()
                .uri("/recipes?ingredientId=" + ingredientId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(BasicRecipeDto.class).isEqualTo(expectedRecipeDtos);
    }

    @Test
    void should_retrieve_recipes_by_property_id() {
        String propertyId = generateId();
        Set<RecipeProjection> recipes = Set.of(
                new RecipeProjection(generateId(), "Black beans", Set.of("https://black.beans.com")),
                new RecipeProjection(generateId(), "Green beans", Set.of("https://green.beans.com")),
                new RecipeProjection(generateId(), "White beans", Set.of("https://white.beans.com")));
        when(queryGateway.streamingQuery(new FindRecipesByQuery(null, propertyId), RecipeProjection.class)).thenReturn(Flux.fromIterable(recipes));
        List<BasicRecipeDto> expectedRecipeDtos = recipes.stream()
                .map(BasicRecipeDto::fromProjection)
                .collect(Collectors.toList());

        webTestClient.get()
                .uri("/recipes?propertyId=" + propertyId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(BasicRecipeDto.class).isEqualTo(expectedRecipeDtos);
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
}
