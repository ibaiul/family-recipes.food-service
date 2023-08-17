package eus.ibai.family.recipes.food.rm.application.controller;

import eus.ibai.family.recipes.food.exception.IngredientNotFoundException;
import eus.ibai.family.recipes.food.rm.application.dto.BasicIngredientDto;
import eus.ibai.family.recipes.food.rm.application.dto.IngredientDto;
import eus.ibai.family.recipes.food.rm.domain.ingredient.FindIngredientByIdQuery;
import eus.ibai.family.recipes.food.rm.domain.ingredient.FindIngredientsByQuery;
import eus.ibai.family.recipes.food.rm.domain.ingredient.IngredientProjection;
import eus.ibai.family.recipes.food.rm.domain.ingredient.IngredientPropertyProjection;
import eus.ibai.family.recipes.food.security.*;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static eus.ibai.family.recipes.food.test.TestUtils.authenticate;
import static eus.ibai.family.recipes.food.test.TestUtils.fixedTime;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {IngredientController.class, AuthController.class})
@Import({SecurityConfig.class, JwtService.class, JwtProperties.class, UserProperties.class})
class IngredientControllerIT {

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
    void should_retrieve_ingredient() {
        IngredientProjection ingredient = new IngredientProjection(generateId(), "Lentils", Set.of(new IngredientPropertyProjection(generateId(), "Legume", fixedTime())));
        when(queryGateway.streamingQuery(new FindIngredientByIdQuery(ingredient.id()), IngredientProjection.class)).thenReturn(Flux.just(ingredient));
        IngredientDto expectedIngredientDto = IngredientDto.fromProjection(ingredient);

        webTestClient.get()
                .uri("/ingredients/" + ingredient.id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(IngredientDto.class).isEqualTo(expectedIngredientDto);
    }

    @Test
    void should_serialize_ingredient_property_date_with_three_digit_millis() {
        String expectedDateTimeFormat = "2023-04-14T22:39:00.200";
        IngredientProjection ingredient = new IngredientProjection(generateId(), "Lentils", Set.of(new IngredientPropertyProjection(generateId(), "Legume", LocalDateTime.parse(expectedDateTimeFormat))));
        when(queryGateway.streamingQuery(new FindIngredientByIdQuery(ingredient.id()), IngredientProjection.class)).thenReturn(Flux.just(ingredient));

        webTestClient.get()
                .uri("/ingredients/" + ingredient.id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody().jsonPath("$.properties[0].addedOn").isEqualTo(expectedDateTimeFormat);
    }

    @Test
    void should_not_retrieve_ingredient_that_does_not_exist() {
        String ingredientId = generateId();
        when(queryGateway.streamingQuery(new FindIngredientByIdQuery(ingredientId), IngredientProjection.class)).thenReturn(Flux.error(new IngredientNotFoundException("")));

        webTestClient.get()
                .uri("/ingredients/" + ingredientId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_not_retrieve_ingredient_when_ingredient_id_invalid() {
        webTestClient.get()
                .uri("/ingredients/ingredientId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void should_retrieve_all_ingredients() {
        Set<IngredientProjection> ingredients = Set.of(
                new IngredientProjection(generateId(), "Black beans"),
                new IngredientProjection(generateId(), "Green beans"),
                new IngredientProjection(generateId(), "White beans"));
        when(queryGateway.streamingQuery(new FindIngredientsByQuery(null), IngredientProjection.class)).thenReturn(Flux.fromIterable(ingredients));
        List<BasicIngredientDto> expectedIngredientDtos = ingredients.stream()
                .map(BasicIngredientDto::fromProjection)
                .collect(Collectors.toList());

        webTestClient.get()
                .uri("/ingredients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(BasicIngredientDto.class).isEqualTo(expectedIngredientDtos);
    }

    @Test
    void should_retrieve_ingredients_by_property_id() {
        String propertyId = generateId();
        Set<IngredientProjection> ingredients = Set.of(
                new IngredientProjection(generateId(), "Black beans"),
                new IngredientProjection(generateId(), "Green beans"),
                new IngredientProjection(generateId(), "White beans"));
        when(queryGateway.streamingQuery(new FindIngredientsByQuery(propertyId), IngredientProjection.class)).thenReturn(Flux.fromIterable(ingredients));
        List<BasicIngredientDto> expectedIngredientDtos = ingredients.stream()
                        .map(BasicIngredientDto::fromProjection)
                                .collect(Collectors.toList());

        webTestClient.get()
                .uri("/ingredients?propertyId=" + propertyId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(BasicIngredientDto.class).isEqualTo(expectedIngredientDtos);
    }

    @Test
    void should_not_retrieve_ingredients_when_query_filters_invalid() {
        webTestClient.get()
                .uri("/ingredients?propertyId=ch>")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
