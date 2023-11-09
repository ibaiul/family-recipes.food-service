package eus.ibai.family.recipes.food.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpHeaders.WWW_AUTHENTICATE;

public class GlobalServerAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        return Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            if (ex instanceof AuthenticationCredentialsNotFoundException) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().set(WWW_AUTHENTICATE, "Bearer");
            } else {
                response.setStatusCode(HttpStatus.FORBIDDEN);
            }
        });
    }
}
