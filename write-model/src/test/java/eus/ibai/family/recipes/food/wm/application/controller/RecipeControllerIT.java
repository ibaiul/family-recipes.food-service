package eus.ibai.family.recipes.food.wm.application.controller;

import eus.ibai.family.recipes.food.exception.RecipeNotFoundException;
import eus.ibai.family.recipes.food.security.*;
import eus.ibai.family.recipes.food.wm.application.dto.AddRecipeIngredientDto;
import eus.ibai.family.recipes.food.wm.application.dto.AddRecipeTagDto;
import eus.ibai.family.recipes.food.wm.application.dto.CreateRecipeDto;
import eus.ibai.family.recipes.food.wm.application.dto.UpdateRecipeDto;
import eus.ibai.family.recipes.food.wm.domain.recipe.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static eus.ibai.family.recipes.food.test.TestUtils.authenticate;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {RecipeController.class, AuthController.class})
@Import({SecurityConfig.class, JwtService.class, JwtProperties.class, UserProperties.class})
class RecipeControllerIT {

    @MockBean
    private RecipeService recipeService;
    
    @Autowired
    private WebTestClient webTestClient;

    private String bearerToken;

    @BeforeEach
    void beforeEach() {
        bearerToken = authenticate(webTestClient).accessToken();
    }

    @Test
    void should_create_recipe() {
        CreateRecipeDto dto = new CreateRecipeDto("Pasta carbonara");
        when(recipeService.createRecipe(dto.recipeName())).thenReturn(Mono.just("recipeId"));

        webTestClient.post()
                .uri("/recipes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectHeader().location("/recipes/recipeId")
                .expectStatus().isCreated();
    }

    @Test
    void should_not_create_recipe_if_already_exists() {
        CreateRecipeDto dto = new CreateRecipeDto("Pasta carbonara");
        when(recipeService.createRecipe(dto.recipeName())).thenReturn(Mono.error(new RecipeAlreadyExistsException("")));

        webTestClient.post()
                .uri("/recipes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_not_create_recipe_if_name_not_provided() {
        CreateRecipeDto dto = new CreateRecipeDto(null);

        webTestClient.post()
                .uri("/recipes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(recipeService);
    }

    @Test
    void should_update_recipe() {
        UpdateRecipeDto dto = new UpdateRecipeDto("Vietnamese curry rice", Set.of("https://rice.com"));
        when(recipeService.updateRecipe("recipeId", dto.recipeName(), dto.recipeLinks())).thenReturn(Mono.empty());

        webTestClient.put()
                .uri("/recipes/recipeId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void should_return_conflict_when_update_recipe_and_new_name_already_exists() {
        UpdateRecipeDto dto = new UpdateRecipeDto("Vietnamese curry rice", Collections.emptySet());
        when(recipeService.updateRecipe("recipeId", dto.recipeName(), dto.recipeLinks())).thenReturn(Mono.error(new RecipeAlreadyExistsException("")));

        webTestClient.put()
                .uri("/recipes/recipeId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_not_update_recipe_if_recipe_does_not_exist() {
        UpdateRecipeDto dto = new UpdateRecipeDto("Vietnamese curry rice", Collections.emptySet());
        when(recipeService.updateRecipe("recipeId", dto.recipeName(), dto.recipeLinks())).thenReturn(Mono.error(new RecipeNotFoundException("")));

        webTestClient.put()
                .uri("/recipes/recipeId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNotFound();
    }

    @ParameterizedTest
    @MethodSource
    void should_not_update_recipe_if_request_body_invalid(UpdateRecipeDto dto) {
        webTestClient.put()
                .uri("/recipes/recipeId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isBadRequest();
    }

    private static Stream<UpdateRecipeDto> should_not_update_recipe_if_request_body_invalid() {
        return Stream.of(
                new UpdateRecipeDto(null, Collections.emptySet()),
                new UpdateRecipeDto("", Collections.emptySet()),
                new UpdateRecipeDto("   ", Collections.emptySet()),
                new UpdateRecipeDto("Penne carbonara", null),
                new UpdateRecipeDto("Penne carbonara", Set.of("https-www.google.com"))
        );
    }

    @Test
    void should_add_ingredient_to_recipe() {
        AddRecipeIngredientDto dto = new AddRecipeIngredientDto("Spaghetti");

        when(recipeService.addRecipeIngredient("recipeId", dto.ingredientName())).thenReturn(Mono.just("ingredientId"));

        webTestClient.post()
                .uri("/recipes/recipeId/ingredients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectHeader().location("/recipes/recipeId/ingredients/ingredientId")
                .expectStatus().isCreated();
    }

    @Test
    void should_not_add_ingredient_to_recipe_if_already_attached() {
        AddRecipeIngredientDto dto = new AddRecipeIngredientDto("Spaghetti");
        when(recipeService.addRecipeIngredient("recipeId", dto.ingredientName())).thenReturn(Mono.error(new RecipeIngredientAlreadyAddedException("")));

        webTestClient.post()
                .uri("/recipes/recipeId/ingredients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_not_add_ingredient_to_recipe_if_recipe_does_not_exist() {
        AddRecipeIngredientDto dto = new AddRecipeIngredientDto("Spaghetti");

        when(recipeService.addRecipeIngredient("recipeId", dto.ingredientName())).thenReturn(Mono.error(new RecipeNotFoundException("")));

        webTestClient.post()
                .uri("/recipes/recipeId/ingredients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_not_add_ingredient_if_ingredient_name_not_provided() {
        AddRecipeIngredientDto dto = new AddRecipeIngredientDto("");

        webTestClient.post()
                .uri("/recipes/recipeId/ingredients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(recipeService);
    }

    @Test
    void should_remove_ingredient_from_recipe() {
        when(recipeService.removeRecipeIngredient("recipeId", "ingredientId")).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/recipes/recipeId/ingredients/ingredientId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void should_not_remove_ingredient_from_recipe_if_recipe_does_not_exist() {
        when(recipeService.removeRecipeIngredient("recipeId", "ingredientId")).thenReturn(Mono.error(new RecipeNotFoundException("")));

        webTestClient.delete()
                .uri("/recipes/recipeId/ingredients/ingredientId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_not_remove_ingredient_from_recipe_if_it_does_not_contain() {
        when(recipeService.removeRecipeIngredient("recipeId", "ingredientId")).thenReturn(Mono.error(new RecipeIngredientNotFoundException("")));

        webTestClient.delete()
                .uri("/recipes/recipeId/ingredients/ingredientId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_add_tag_to_recipe() {
        AddRecipeTagDto dto = new AddRecipeTagDto("tagName");

        when(recipeService.addRecipeTag("recipeId", dto.tag())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/recipes/recipeId/tags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void should_not_add_tag_if_recipe_does_not_exist() {
        AddRecipeTagDto dto = new AddRecipeTagDto("First course");

        when(recipeService.addRecipeTag("recipeId", dto.tag())).thenReturn(Mono.error(new RecipeNotFoundException("")));

        webTestClient.post()
                .uri("/recipes/recipeId/tags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNotFound();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    void should_not_add_tag_if_tag_name_not_provided(String tag) {
        AddRecipeTagDto dto = new AddRecipeTagDto(tag);

        webTestClient.post()
                .uri("/recipes/recipeId/tags")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(recipeService);
    }

    @Test
    void should_remove_tag_from_recipe() {
        when(recipeService.removeRecipeTag("recipeId", "tagName")).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/recipes/recipeId/tags?name=tag")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void should_not_remove_tag_if_recipe_does_not_exist() {
        when(recipeService.removeRecipeTag("recipeId", "tagName")).thenReturn(Mono.error(new RecipeNotFoundException("")));

        webTestClient.delete()
                .uri("/recipes/recipeId/tags?name=tagName")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_not_remove_tag_if_it_does_not_contain() {
        when(recipeService.removeRecipeTag("recipeId", "tagName")).thenReturn(Mono.error(new RecipeTagNotFoundException("")));

        webTestClient.delete()
                .uri("/recipes/recipeId/tags?name=tagName")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"name=", "name=  "})
    void should_provide_valid_tag_name_when_deleting_tag_from_recipe(String params) {
        webTestClient.delete()
                .uri("/recipes/recipeId/tags?" + params)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void should_delete_recipe() {
        when(recipeService.deleteRecipe("recipeId")).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/recipes/recipeId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void should_not_delete_recipe_if_does_not_exist() {
        when(recipeService.deleteRecipe("recipeId")).thenReturn(Mono.error(new RecipeNotFoundException("")));

        webTestClient.delete()
                .uri("/recipes/recipeId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }
}
