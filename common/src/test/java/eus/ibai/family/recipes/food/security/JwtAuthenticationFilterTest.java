package eus.ibai.family.recipes.food.security;

import eus.ibai.family.recipes.food.test.TestWebFilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Spy
    private TestWebFilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter authenticationFilter;

    @Test
    void should_forward_requests_without_authentication() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

        authenticationFilter.filter(exchange, filterChain)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
        assertThat(filterChain.getAuthenticatedRole()).isNull();
        verifyNoInteractions(jwtService);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(exchange.getResponse().getHeaders()).doesNotContainKey(HttpHeaders.WWW_AUTHENTICATE);
    }

    @Test
    void should_authenticate_requests() {
        String expectedRole = "ROLE_FAMILY_MEMBER";
        String validToken = "validToken";
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken));
        when(jwtService.getUserDetails(validToken)).thenReturn(Mono.just(Tuples.of("username", List.of(expectedRole))));

        authenticationFilter.filter(exchange, filterChain)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(filterChain, times(1)).filter(exchange);
        assertThat(filterChain.getAuthenticatedRole()).isEqualTo(expectedRole);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
        assertThat(exchange.getResponse().getHeaders()).doesNotContainKey(HttpHeaders.WWW_AUTHENTICATE);
    }
}