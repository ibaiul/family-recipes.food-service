package eus.ibai.family.recipes.food.auth.service;

import eus.ibai.family.recipes.food.security.JwtResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@RequiredArgsConstructor
public class PeriodicallyRefreshedServiceTokenProvider implements ServiceTokenProvider {

    private final ServiceTokenAuthenticator serviceTokenAuthenticator;

    private final ScheduledThreadPoolExecutor executorService;

    private final long refreshPeriodSeconds;

    private final AtomicReference<JwtResponseDto> jwtTokens = new AtomicReference<>();

    public void init() {
        log.debug("Authenticating service instance.");
        serviceTokenAuthenticator.authenticateServiceInstance()
                .doAfterTerminate(() -> {
                    if (executorService.getTaskCount() == 0) {
                        scheduleTokenRefresh();
                    }
                }).subscribe(jwtTokens::set);
    }

    private void scheduleTokenRefresh() {
        log.debug("Scheduling service instance credentials refresh.");
        executorService.scheduleWithFixedDelay(
                () -> serviceTokenAuthenticator.refreshServiceInstanceCredentials(jwtTokens.get().refreshToken())
                        .onErrorResume(t -> serviceTokenAuthenticator.authenticateServiceInstance())
                        .subscribe(jwtTokens::set),
                refreshPeriodSeconds, refreshPeriodSeconds, SECONDS);
    }

    public Optional<String> getServiceToken() {
        return Optional.ofNullable(jwtTokens.get())
                .map(JwtResponseDto::accessToken);
    }
}
