package eus.ibai.family.recipes.food.wm.application.controller;

import eus.ibai.family.recipes.food.exception.IngredientNotFoundException;
import eus.ibai.family.recipes.food.security.*;
import eus.ibai.family.recipes.food.wm.application.dto.AddIngredientPropertyDto;
import eus.ibai.family.recipes.food.wm.application.dto.CreateIngredientDto;
import eus.ibai.family.recipes.food.wm.application.dto.UpdateIngredientDto;
import eus.ibai.family.recipes.food.wm.domain.ingredient.*;
import eus.ibai.family.recipes.food.wm.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

import java.util.stream.Stream;

import static eus.ibai.family.recipes.food.test.TestUtils.authenticate;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {IngredientController.class, AuthController.class})
@Import({GlobalSecurityConfig.class, SecurityConfig.class, JwtService.class, JwtProperties.class, UserProperties.class})
class IngredientControllerIT {

    @MockBean
    private IngredientService ingredientService;

    @Autowired
    private WebTestClient webTestClient;

    private String bearerToken;

    @BeforeEach
    void beforeEach() {
        bearerToken = authenticate(webTestClient).accessToken();
    }

    @Test
    void should_create_ingredient() {
        CreateIngredientDto dto = new CreateIngredientDto("Spaghetti");
        when(ingredientService.createIngredient(dto.ingredientName())).thenReturn(Mono.just("ingredientId"));

        webTestClient.post()
                .uri("/ingredients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectHeader().location("/ingredients/ingredientId")
                .expectStatus().isCreated();
    }

    @Test
    void should_not_create_ingredient_if_already_exists() {
        CreateIngredientDto dto = new CreateIngredientDto("Tomato");
        when(ingredientService.createIngredient(dto.ingredientName())).thenReturn(Mono.error(new IngredientAlreadyExistsException("")));

        webTestClient.post()
                .uri("/ingredients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @ParameterizedTest
    @MethodSource("invalidNameProvider")
    void should_not_create_ingredient_if_invalid_name_provided(String invalidName) {
        CreateIngredientDto dto = new CreateIngredientDto(invalidName);

        webTestClient.post()
                .uri("/ingredients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(ingredientService);
    }

    @Test
    void should_update_ingredient() {
        UpdateIngredientDto dto = new UpdateIngredientDto("Rice");
        when(ingredientService.updateIngredient("ingredientId", dto.ingredientName())).thenReturn(Mono.empty());

        webTestClient.put()
                .uri("/ingredients/ingredientId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void should_return_conflict_when_update_ingredient_and_new_name_already_exists() {
        UpdateIngredientDto dto = new UpdateIngredientDto("Rice");
        when(ingredientService.updateIngredient("ingredientId", dto.ingredientName())).thenReturn(Mono.error(new IngredientAlreadyExistsException("")));

        webTestClient.put()
                .uri("/ingredients/ingredientId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_not_update_ingredient_if_ingredient_does_not_exist() {
        UpdateIngredientDto dto = new UpdateIngredientDto("Rice");
        when(ingredientService.updateIngredient("ingredientId", dto.ingredientName())).thenReturn(Mono.error(new IngredientNotFoundException("")));

        webTestClient.put()
                .uri("/ingredients/ingredientId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNotFound();
    }

    @ParameterizedTest
    @MethodSource("invalidNameProvider")
    void should_not_update_ingredient_if_request_body_invalid(String invalidName) {
        UpdateIngredientDto dto = new UpdateIngredientDto(invalidName);

        webTestClient.put()
                .uri("/ingredients/ingredientId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void should_add_property_to_ingredient() {
        AddIngredientPropertyDto dto = new AddIngredientPropertyDto("Spaghetti");

        when(ingredientService.addIngredientProperty("ingredientId", dto.propertyName())).thenReturn(Mono.just("propertyId"));

        webTestClient.post()
                .uri("/ingredients/ingredientId/properties")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectHeader().location("/ingredients/ingredientId/properties/propertyId")
                .expectStatus().isCreated();
    }

    @Test
    void should_not_add_property_to_ingredient_if_already_added() {
        AddIngredientPropertyDto dto = new AddIngredientPropertyDto("Spaghetti");

        when(ingredientService.addIngredientProperty("ingredientId", dto.propertyName())).thenReturn(Mono.error(new IngredientPropertyAlreadyAddedException("")));

        webTestClient.post()
                .uri("/ingredients/ingredientId/properties")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_not_add_property_to_ingredient_if_ingredient_does_not_exist() {
        AddIngredientPropertyDto dto = new AddIngredientPropertyDto("Spaghetti");

        when(ingredientService.addIngredientProperty("ingredientId", dto.propertyName())).thenReturn(Mono.error(new IngredientNotFoundException("")));

        webTestClient.post()
                .uri("/ingredients/ingredientId/properties")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNotFound();
    }

    @ParameterizedTest
    @MethodSource("invalidNameProvider")
    void should_not_add_property_if_property_name_not_provided(String invalidPropertyName) {
        AddIngredientPropertyDto dto = new AddIngredientPropertyDto(invalidPropertyName);

        webTestClient.post()
                .uri("/ingredients/ingredientId/properties")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(ingredientService);
    }

    @Test
    void should_remove_property_from_ingredient() {
        when(ingredientService.removeIngredientProperty("ingredientId", "propertyId")).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/ingredients/ingredientId/properties/propertyId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void should_not_remove_property_from_ingredient_if_ingredient_does_not_exists() {
        when(ingredientService.removeIngredientProperty("ingredientId", "propertyId")).thenReturn(Mono.error(new IngredientNotFoundException("")));

        webTestClient.delete()
                .uri("/ingredients/ingredientId/properties/propertyId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_not_remove_property_from_ingredient_if_it_does_not_contain() {
        when(ingredientService.removeIngredientProperty("ingredientId", "propertyId")).thenReturn(Mono.error(new IngredientPropertyNotFoundException("")));

        webTestClient.delete()
                .uri("/ingredients/ingredientId/properties/propertyId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_delete_ingredient() {
        when(ingredientService.deleteIngredient("ingredientId")).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/ingredients/ingredientId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void should_not_delete_ingredient_if_does_not_exist() {
        when(ingredientService.deleteIngredient("ingredientId")).thenReturn(Mono.error(new IngredientNotFoundException("")));

        webTestClient.delete()
                .uri("/ingredients/ingredientId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_not_delete_ingredient_if_attached_to_a_recipe() {
        when(ingredientService.deleteIngredient("ingredientId")).thenReturn(Mono.error(new IngredientAttachedToRecipeException("")));

        webTestClient.delete()
                .uri("/ingredients/ingredientId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    private static Stream<String> invalidNameProvider() {
        return Stream.of(null, "", "   ");
    }
}
