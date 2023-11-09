package eus.ibai.family.recipes.food.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * NOTE: We could implement AuthenticationWebFilter and provide ServerAuthenticationFailureHandler and ServerAuthenticationConverter
 */

@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private static final String ACCESS_TOKEN_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return Mono.justOrEmpty(authHeader)
                .filter(header -> header.startsWith(ACCESS_TOKEN_PREFIX))
                .flatMap(this::getAuthentication)
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange).then(Mono.empty())))
                .flatMap(authentication -> Mono.defer(() -> chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))))
                .onErrorResume(InvalidJwtTokenException.class, t -> {
                    log.trace("Authentication failed: {}", t.getMessage());
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    exchange.getResponse().getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                    return Mono.empty();
                });
    }

    private Mono<UsernamePasswordAuthenticationToken> getAuthentication(String authHeader) {
        return Mono.just(authHeader)
                .map(header -> header.replace(ACCESS_TOKEN_PREFIX, ""))
                .flatMap(jwtService::getUserDetails)
                .map(userDetails -> {
                    Collection<SimpleGrantedAuthority> authorities = userDetails.getT2().stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    return new UsernamePasswordAuthenticationToken(userDetails.getT1(), null, authorities);
                });
    }
}
