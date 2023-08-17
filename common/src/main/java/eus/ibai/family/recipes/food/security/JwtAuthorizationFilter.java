package eus.ibai.family.recipes.food.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Collection;
import java.util.List;

/**
 * NOTE: We could implement AuthenticationWebFilter and provide ServerAuthenticationFailureHandler and ServerAuthenticationConverter
 */

@Slf4j
public class JwtAuthorizationFilter implements WebFilter {

    private static final String ACCESS_TOKEN_PREFIX = "Bearer ";

    private final JwtService jwtService;

    private final List<Tuple2<HttpMethod, PathPattern>> authAllowList;

    public JwtAuthorizationFilter(JwtService jwtService, List<Tuple2<HttpMethod, String>> authAllowList) {
        this.jwtService = jwtService;
        this.authAllowList = authAllowList.stream()
                .map(t -> Tuples.of(t.getT1(), new PathPatternParser().parse(t.getT2())))
                .toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return Mono.justOrEmpty(authHeader)
                .filter(header -> header.startsWith(ACCESS_TOKEN_PREFIX))
                .switchIfEmpty(Mono.defer(() -> requestDoesNotRequireAuth(exchange.getRequest())
                        .filter(notAuthRequired -> notAuthRequired)
                        .switchIfEmpty(Mono.defer(() -> unauthorizedResponse(exchange).then(Mono.empty())))
                        .flatMap(notAuthRequired -> Mono.defer(() -> chain.filter(exchange)))
                        .then(Mono.empty())))
                .flatMap(this::getAuthentication)
                .flatMap(authentication -> chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
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
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .toList();
                    return new UsernamePasswordAuthenticationToken(userDetails.getT1(), null, authorities);
                });
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        return Mono.empty();
    }

    private Mono<Boolean> requestDoesNotRequireAuth(ServerHttpRequest request) {
        return Flux.fromIterable(authAllowList)
                .any(allowList -> allowList.getT2().matches(request.getPath().pathWithinApplication()) && allowList.getT1() == request.getMethod());
    }

    public List<Tuple2<HttpMethod, String>> getAuthAllowList() {
        return authAllowList.stream()
                .map(t -> Tuples.of(t.getT1(), t.getT2().getPatternString()))
                .toList();
    }
}
