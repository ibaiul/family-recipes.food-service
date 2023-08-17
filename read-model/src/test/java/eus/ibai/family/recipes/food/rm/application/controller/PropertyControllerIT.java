package eus.ibai.family.recipes.food.rm.application.controller;

import eus.ibai.family.recipes.food.exception.PropertyNotFoundException;
import eus.ibai.family.recipes.food.rm.application.dto.BasicPropertyDto;
import eus.ibai.family.recipes.food.rm.domain.property.FindAllPropertiesQuery;
import eus.ibai.family.recipes.food.rm.domain.property.FindPropertyByIdQuery;
import eus.ibai.family.recipes.food.rm.domain.property.PropertyProjection;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static eus.ibai.family.recipes.food.test.TestUtils.authenticate;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = {PropertyController.class, AuthController.class})
@Import({SecurityConfig.class, JwtService.class, JwtProperties.class, UserProperties.class})
class PropertyControllerIT {

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
    void should_retrieve_all_properties() {
        Set<PropertyProjection> properties = Set.of(
                new PropertyProjection(generateId(), "Vitamin C"),
                new PropertyProjection(generateId(), "Calcium"),
                new PropertyProjection(generateId(), "Fiber"));
        when(queryGateway.streamingQuery(new FindAllPropertiesQuery(), PropertyProjection.class)).thenReturn(Flux.fromIterable(properties));
        List<BasicPropertyDto> expectedPropertyDtos = properties.stream()
                        .map(BasicPropertyDto::fromProjection)
                                .collect(Collectors.toList());

        webTestClient.get()
                .uri("/properties")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(BasicPropertyDto.class).isEqualTo(expectedPropertyDtos);
    }

    @Test
    void should_retrieve_property() {
        PropertyProjection property = new PropertyProjection(generateId(), "Vitamin C");
        when(queryGateway.streamingQuery(new FindPropertyByIdQuery(property.id()), PropertyProjection.class)).thenReturn(Flux.just(property));
        BasicPropertyDto expectedPropertyDto = BasicPropertyDto.fromProjection(property);

        webTestClient.get()
                .uri("/properties/" + property.id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(BasicPropertyDto.class).isEqualTo(expectedPropertyDto);
    }

    @Test
    void should_not_retrieve_property_that_does_not_exist() {
        String propertyId = generateId();
        when(queryGateway.streamingQuery(new FindPropertyByIdQuery(propertyId), PropertyProjection.class)).thenReturn(Flux.error(new PropertyNotFoundException("")));

        webTestClient.get()
                .uri("/properties/" + propertyId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void should_not_retrieve_property_when_property_id_invalid() {
        webTestClient.get()
                .uri("/properties/propertyId")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
