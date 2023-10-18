package eus.ibai.family.recipes.food.auth.service;

import eus.ibai.family.recipes.food.security.InvalidJwtTokenException;
import eus.ibai.family.recipes.food.security.JwtResponseDto;
import eus.ibai.family.recipes.food.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;

@ExtendWith(MockitoExtension.class)
class LocalServiceTokenAuthenticatorTest {

    private final String serviceId = "serviceId";

    @Mock
    private JwtService jwtService;

    private ServiceTokenAuthenticator serviceTokenAuthenticator;

    @BeforeEach
    void beforeEach() {
        serviceTokenAuthenticator = new LocalServiceTokenAuthenticator(serviceId, jwtService);
    }

    @Test
    void should_return_service_tokens_when_authenticating() {
        JwtResponseDto expectedJwtTokens = new JwtResponseDto("accessToken", "tokenType", 0, "refreshToken");
        when(jwtService.create(serviceId)).thenReturn(Mono.just(expectedJwtTokens));

        serviceTokenAuthenticator.authenticateServiceInstance()
                .as(StepVerifier::create)
                .expectNext(expectedJwtTokens)
                .verifyComplete();
    }

    @Test
    void should_propagate_error_when_authentication_fails() {
        UsernameNotFoundException expectedError = new UsernameNotFoundException("Error message");
        when(jwtService.create(serviceId)).thenReturn(Mono.error(expectedError));

        serviceTokenAuthenticator.authenticateServiceInstance()
                .as(StepVerifier::create)
                .expectErrorMatches(t -> t.getClass().equals(expectedError.getClass()) && t.getMessage().equals(expectedError.getMessage()))
                .verify();
    }

    @Test
    void should_return_service_tokens_when_refreshing_credentials() {
        String initialRefreshToken = "initialRefreshToken";
        JwtResponseDto expectedJwtTokens = new JwtResponseDto("accessToken", "tokenType", 0, "refreshToken");
        when(jwtService.refresh(initialRefreshToken)).thenReturn(Mono.just(expectedJwtTokens));

        serviceTokenAuthenticator.refreshServiceInstanceCredentials(initialRefreshToken)
                .as(StepVerifier::create)
                .expectNext(expectedJwtTokens)
                .verifyComplete();
    }

    @Test
    void should_propagate_error_when_refreshing_credentials_fails() {
        String refreshToken = "refreshToken";
        WebClientRequestException expectedError = new WebClientRequestException(new InvalidJwtTokenException("Error message"), POST, mock(URI.class), new HttpHeaders());
        when(jwtService.refresh(refreshToken)).thenReturn(Mono.error(expectedError));

        serviceTokenAuthenticator.refreshServiceInstanceCredentials(refreshToken)
                .as(StepVerifier::create)
                .expectErrorMatches(t -> t.getClass().equals(expectedError.getClass()) && t.getMessage().equals(expectedError.getMessage()))
                .verify();
    }
}