package eus.ibai.family.recipes.food.rm.application.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import eus.ibai.family.recipes.food.event.DomainEvent;
import eus.ibai.family.recipes.food.event.IngredientCreatedEvent;
import eus.ibai.family.recipes.food.event.RecipeCreatedEvent;
import eus.ibai.family.recipes.food.rm.infrastructure.config.SecurityConfig;
import eus.ibai.family.recipes.food.security.*;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static eus.ibai.family.recipes.food.rm.test.TestUtils.OBJECT_MAPPER;
import static eus.ibai.family.recipes.food.test.TestUtils.authenticate;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

@WebFluxTest(controllers = {SseController.class, AuthController.class})
@Import({GlobalSecurityConfig.class, SecurityConfig.class, JwtService.class, JwtProperties.class, UserProperties.class})
class SseControllerIT {

    @MockBean
    private SseEventPublisher sseEventPublisher;

    @Autowired
    private WebTestClient webTestClient;

    private String bearerToken;

    @BeforeEach
    void beforeEach() {
        bearerToken = authenticate(webTestClient).accessToken();
    }

    @Test
    void should_receive_published_domain_events_when_subscribing_to_sse_publisher() throws JsonProcessingException {
        RecipeCreatedEvent recipeCreatedEvent = new RecipeCreatedEvent(generateId(), "Pil-pil cod");
        IngredientCreatedEvent ingredientCreatedEvent = new IngredientCreatedEvent(generateId(), "Cod");
        ServerSentEvent<DomainEvent<String>> expectedSseEvent1 = ServerSentEvent.<DomainEvent<String>>builder()
                .id(generateId())
                .event(RecipeCreatedEvent.class.getSimpleName())
                .data(recipeCreatedEvent)
                .build();
        ServerSentEvent<DomainEvent<String>> expectedSseEvent2 = ServerSentEvent.<DomainEvent<String>>builder()
                .id(generateId())
                .event(IngredientCreatedEvent.class.getSimpleName())
                .data(ingredientCreatedEvent)
                .build();
        when(sseEventPublisher.createSubscription()).thenReturn(Flux.fromIterable(List.of(expectedSseEvent1, expectedSseEvent2)));
        ParameterizedTypeReference<ServerSentEvent<DomainEvent<String>>> type = new ParameterizedTypeReference<>() {};

        List<ServerSentEvent<DomainEvent<String>>> serverSentEvents = webTestClient
                .mutate()
                .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs()
                        .jackson2JsonDecoder(new Jackson2JsonDecoder(OBJECT_MAPPER, MediaType.APPLICATION_JSON)))
                .build()
                .get()
                .uri("/events/sse")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .accept(TEXT_EVENT_STREAM)
                .exchange()
                .expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
                .expectHeader().cacheControl(CacheControl.noCache())
                .expectHeader().valueEquals("X-Accel-Buffering", "no")
                .expectHeader().valueEquals("Connection", "keep-alive")
                .expectBodyList(type)
                .returnResult()
                .getResponseBody();

        assertAll(
                () -> org.assertj.core.api.Assertions.assertThat(serverSentEvents).hasSize(2),
                () -> org.assertj.core.api.Assertions.assertThat(serverSentEvents.get(0)).matches(sse -> equals(sse, expectedSseEvent1)),
                () -> org.assertj.core.api.Assertions.assertThat(serverSentEvents.get(1)).matches(sse -> equals(sse, expectedSseEvent2))
        );
    }

    @SneakyThrows
    private boolean equals(ServerSentEvent<DomainEvent<String>> sse1, ServerSentEvent<DomainEvent<String>> sse2) {
        return sse1.id().equals(sse2.id()) &&
                sse1.event().equals(sse2.event()) &&
                sse1.data().equals(sse2.data());
    }
}
