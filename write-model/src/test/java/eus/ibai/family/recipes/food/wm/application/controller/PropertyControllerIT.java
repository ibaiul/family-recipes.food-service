package eus.ibai.family.recipes.food.wm.application.controller;

import eus.ibai.family.recipes.food.exception.PropertyNotFoundException;
import eus.ibai.family.recipes.food.security.*;
import eus.ibai.family.recipes.food.wm.application.dto.CreatePropertyDto;
import eus.ibai.family.recipes.food.wm.application.dto.UpdatePropertyDto;
import eus.ibai.family.recipes.food.wm.domain.property.PropertyAlreadyExistsException;
import eus.ibai.family.recipes.food.wm.domain.property.PropertyAttachedToIngredientException;
import eus.ibai.family.recipes.food.wm.domain.property.PropertyService;
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

@WebFluxTest(controllers = {PropertyController.class, AuthController.class})
@Import({SecurityConfig.class, JwtService.class, JwtProperties.class, UserProperties.class})
class PropertyControllerIT {

    @MockBean
    private PropertyService propertyService;

    @Autowired
    private WebTestClient webTestClient;

    private String bearerToken;

    @BeforeEach
    void beforeEach() {
        bearerToken = authenticate(webTestClient).accessToken();
    }

    @Test
    void should_create_property() {
        CreatePropertyDto dto = new CreatePropertyDto("Vitamin C");
        when(propertyService.createProperty("Vitamin C")).thenReturn(Mono.just("propertyId"));

        webTestClient.post()
                .uri("/properties")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectHeader().location("/properties/propertyId")
                .expectStatus().isCreated();
    }

    @Test
    void should_not_create_property_if_already_exists() {
        CreatePropertyDto dto = new CreatePropertyDto("Vitamin C");
        when(propertyService.createProperty("Vitamin C")).thenReturn(Mono.error(new PropertyAlreadyExistsException("")));

        webTestClient.post()
                .uri("/properties")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @ParameterizedTest
    @MethodSource
    void should_not_create_property_if_request_body_invalid(CreatePropertyDto dto) {
        webTestClient.post()
                .uri("/properties")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(propertyService);
    }

    private static Stream<CreatePropertyDto> should_not_create_property_if_request_body_invalid() {
        return Stream.of(
                new CreatePropertyDto(null),
                new CreatePropertyDto(""),
                new CreatePropertyDto("  ")
        );
    }

    @Test
    void should_update_property() {
        UpdatePropertyDto dto = new UpdatePropertyDto("Vitamin C");
        when(propertyService.updateProperty("propertyId", dto.propertyName())).thenReturn(Mono.empty());

        webTestClient.put()
                .uri("/properties/propertyId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void should_return_conflict_when_update_property_and_new_name_already_exists() {
        UpdatePropertyDto dto = new UpdatePropertyDto("Vitamin C");
        when(propertyService.updateProperty("propertyId", dto.propertyName())).thenReturn(Mono.error(new PropertyAlreadyExistsException("")));

        webTestClient.put()
                .uri("/properties/propertyId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void should_not_update_property_if_property_does_not_exist() {
        UpdatePropertyDto dto = new UpdatePropertyDto("Vitamin C");
        when(propertyService.updateProperty("propertyId", dto.propertyName())).thenReturn(Mono.error(new PropertyNotFoundException("")));

        webTestClient.put()
                .uri("/properties/propertyId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isNotFound();
    }

    @ParameterizedTest
    @MethodSource
    void should_not_update_property_if_request_body_invalid(UpdatePropertyDto dto) {
        webTestClient.put()
                .uri("/properties/propertyId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(dto))
                .exchange()
                .expectStatus().isBadRequest();
    }

    private static Stream<UpdatePropertyDto> should_not_update_property_if_request_body_invalid() {
        return Stream.of(
                new UpdatePropertyDto(null),
                new UpdatePropertyDto(""),
                new UpdatePropertyDto("   ")
        );
    }

    @Test
    void should_delete_property() {
        when(propertyService.deleteProperty("propertyId")).thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/properties/propertyId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void should_not_delete_property_if_does_not_exist() {
        when(propertyService.deleteProperty("propertyId")).thenReturn(Mono.error(new PropertyNotFoundException("")));

        webTestClient.delete()
                .uri("/properties/propertyId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_not_delete_property_if_attached_to_an_ingredient() {
        when(propertyService.deleteProperty("propertyId")).thenReturn(Mono.error(new PropertyAttachedToIngredientException("")));

        webTestClient.delete()
                .uri("/properties/propertyId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }
}
