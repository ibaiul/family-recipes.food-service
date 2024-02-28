package eus.ibai.family.recipes.food.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@SpringBootTest(webEnvironment = MOCK)
class GlobalErrorWebExcetionHandlerTest {

    @Autowired
    private GlobalErrorWebExceptionHandler exceptionHandler;

    @Test
    void should_map_jwt_authentication_exceptions() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        exceptionHandler.handle(exchange, new InvalidJwtTokenException(""))
                .as(StepVerifier::create)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders()).containsKey(HttpHeaders.WWW_AUTHENTICATE);
    }

    @Test
    void should_map_uncaught_exceptions() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        exceptionHandler.handle(exchange, new RuntimeException(""))
                .as(StepVerifier::create)
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}