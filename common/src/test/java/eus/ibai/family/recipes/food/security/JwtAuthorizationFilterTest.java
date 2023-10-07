package eus.ibai.family.recipes.food.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpMethod.GET;

@ExtendWith(MockitoExtension.class)
class JwtAuthorizationFilterTest {

    private final List<PathAuthorization> publicPaths = List.of(
            new PathAuthorization(GET, "/public-endpoint-1"),
            new PathAuthorization(GET, "/public-endpoint-2"),
            new PathAuthorization(GET, "/public-endpoint-prefix/**")
    );

    @Mock
    WebFilterChain filterChain;

    @Mock
    private JwtService jwtService;

    private JwtAuthorizationFilter authorizationFilter;

    @BeforeEach
    void beforeEach() {
        authorizationFilter = new JwtAuthorizationFilter(jwtService, publicPaths);
    }

    @ParameterizedTest
    @MethodSource
    void should_allow_requests_to_public_endpoints_without_authentication(String url) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(url));
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        authorizationFilter.filter(exchange, filterChain)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(exchange.getResponse().getHeaders()).doesNotContainKey(HttpHeaders.WWW_AUTHENTICATE);
    }

    private static Stream<String> should_allow_requests_to_public_endpoints_without_authentication() {
        return Stream.of("/public-endpoint-1", "/public-endpoint-2", "/public-endpoint-prefix/foo");
    }

    @Test
    void should_allow_requests_to_protected_endpoints_with_authentication() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/protected-endpoint-1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer validToken"));
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());
        when(jwtService.getUserDetails("validToken")).thenReturn(Mono.just(Tuples.of("username", List.of("ROLE_FAMILY_MEMBER"))));

        authorizationFilter.filter(exchange, filterChain)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(exchange.getResponse().getHeaders()).doesNotContainKey(HttpHeaders.WWW_AUTHENTICATE);
    }

    @Test
    void should_not_allow_requests_to_protected_endpoints_without_authentication() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/protected-endpoint-1"));

        authorizationFilter.filter(exchange, filterChain)
                .as(StepVerifier::create)
                .verifyComplete();

        verifyNoInteractions(filterChain);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    }
}