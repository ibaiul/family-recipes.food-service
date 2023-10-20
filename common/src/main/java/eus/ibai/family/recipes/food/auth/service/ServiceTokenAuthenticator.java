package eus.ibai.family.recipes.food.auth.service;

import eus.ibai.family.recipes.food.security.JwtResponseDto;
import reactor.core.publisher.Mono;

public interface ServiceTokenAuthenticator {

    Mono<JwtResponseDto> authenticateServiceInstance();

    Mono<JwtResponseDto> refreshServiceInstanceCredentials(String refreshToken);
}
