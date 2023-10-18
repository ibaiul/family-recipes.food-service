package eus.ibai.family.recipes.food.auth.service;

import eus.ibai.family.recipes.food.security.JwtResponseDto;
import eus.ibai.family.recipes.food.security.JwtService;
import eus.ibai.family.recipes.food.util.Temporary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Temporary("Will be re-implemented to perform authentication against a dedicated microservice in charge of identities")
public class LocalServiceTokenAuthenticator implements ServiceTokenAuthenticator {

    private final String serviceId;

    private final JwtService jwtService;

    public Mono<JwtResponseDto> authenticateServiceInstance() {
        return jwtService.create(serviceId)
                .doOnNext(jwtResponseDto -> log.debug("Authenticated service instance."))
                .doOnError(t -> log.error("Failed to authenticate service instance.", t));
    }

    public Mono<JwtResponseDto> refreshServiceInstanceCredentials(String refreshToken) {
        return jwtService.refresh(refreshToken)
                .doOnNext(jwtResponseDto -> log.trace("Refreshed service instance credentials."))
                .doOnError(t -> log.error("Failed to refresh service instance credentials.", t));
    }
}
