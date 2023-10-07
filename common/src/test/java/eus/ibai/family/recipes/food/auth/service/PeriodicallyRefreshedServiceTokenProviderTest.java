package eus.ibai.family.recipes.food.auth.service;

import eus.ibai.family.recipes.food.security.JwtResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import reactor.core.publisher.Mono;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PeriodicallyRefreshedServiceTokenProviderTest {

    @Mock
    private ServiceTokenAuthenticator tokenAuthenticator;

    @Spy
    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private long refreshPeriodSeconds = 1;

    private PeriodicallyRefreshedServiceTokenProvider tokenProvider;

    @BeforeEach
    void beforeEach() {
        tokenProvider = new PeriodicallyRefreshedServiceTokenProvider(tokenAuthenticator, executor, refreshPeriodSeconds);
    }

    @Test
    void should_authenticate_when_initialized() {
        JwtResponseDto expectedJwtTokens = new JwtResponseDto("accessToken", "tokenType", 0, "refreshToken");
        when(tokenAuthenticator.authenticateServiceInstance()).thenReturn(Mono.just(expectedJwtTokens));

        tokenProvider.init();

        await().atMost(1, SECONDS).untilAsserted(() -> {
            assertThat(tokenProvider.getServiceToken()).contains(expectedJwtTokens.accessToken());
            verify(tokenAuthenticator).authenticateServiceInstance();
        });
    }

    @Test
    void should_schedule_token_refresh_when_initialized() {
        JwtResponseDto expectedJwtTokens = new JwtResponseDto("accessToken", "tokenType", 0, "refreshToken");
        when(tokenAuthenticator.authenticateServiceInstance()).thenReturn(Mono.just(expectedJwtTokens));

        tokenProvider.init();

        await().atMost(1, SECONDS).untilAsserted(() -> {
            verify(executor).scheduleWithFixedDelay(any(), eq(refreshPeriodSeconds), eq(refreshPeriodSeconds), eq(SECONDS));
        });
    }

    @Test
    void should_schedule_token_refresh_only_once_when_initialized_multiple_times() {
        JwtResponseDto expectedJwtTokens = new JwtResponseDto("accessToken", "tokenType", 0, "refreshToken");
        when(tokenAuthenticator.authenticateServiceInstance()).thenReturn(Mono.just(expectedJwtTokens));

        tokenProvider.init();
        tokenProvider.init();
        tokenProvider.init();
        tokenProvider.init();
        tokenProvider.init();

        await().atMost(1, SECONDS).untilAsserted(() -> {
            verify(executor, times(1)).scheduleWithFixedDelay(any(), anyLong(), anyLong(), any());
            assertThat(executor.getTaskCount()).isEqualTo(1);
            assertThat(executor.getQueue()).hasSize(1);
        });
    }

    @Test
    void should_schedule_token_refresh_when_initialization_fails() {
        when(tokenAuthenticator.authenticateServiceInstance()).thenReturn(Mono.error(new UsernameNotFoundException("")));

        tokenProvider.init();

        await().atMost(1, SECONDS).untilAsserted(() -> {
            assertThat(tokenProvider.getServiceToken()).isEmpty();
            verify(executor).scheduleWithFixedDelay(any(), eq(refreshPeriodSeconds), eq(refreshPeriodSeconds), eq(SECONDS));
        });
    }

    @Test
    void should_authenticate_again_when_fails_to_refresh_credentials() {
        JwtResponseDto expectedJwtTokens = new JwtResponseDto("accessToken", "tokenType", 0, "refreshToken");
        when(tokenAuthenticator.authenticateServiceInstance()).thenReturn(Mono.just(expectedJwtTokens));
        when(tokenAuthenticator.refreshServiceInstanceCredentials(expectedJwtTokens.refreshToken())).thenReturn(Mono.error(new RuntimeException()));

        tokenProvider.init();

        await().atMost(refreshPeriodSeconds * 2, SECONDS).untilAsserted(() -> {
            verify(tokenAuthenticator, times(2)).authenticateServiceInstance();
        });
    }

    @Test
    void should_refresh_credentials_periodically() {
        JwtResponseDto initialJwtTokens = new JwtResponseDto("initialAccessToken", "tokenType", 0, "initialRefreshToken");
        JwtResponseDto expectedJwtTokens = new JwtResponseDto("accessToken", "tokenType", 0, "refreshToken");
        when(tokenAuthenticator.authenticateServiceInstance()).thenReturn(Mono.just(initialJwtTokens));
        when(tokenAuthenticator.refreshServiceInstanceCredentials(initialJwtTokens.refreshToken())).thenReturn(Mono.just(expectedJwtTokens));

        tokenProvider.init();

        await().atMost(refreshPeriodSeconds * 3, SECONDS).untilAsserted(() -> {
            assertThat(tokenProvider.getServiceToken()).contains(expectedJwtTokens.accessToken());
        });
    }

    @Test
    void should_not_return_service_token_when_unavailable() {
        assertThat(tokenProvider.getServiceToken()).isEmpty();
    }
}