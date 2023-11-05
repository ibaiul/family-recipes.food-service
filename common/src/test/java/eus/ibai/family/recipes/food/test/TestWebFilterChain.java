package eus.ibai.family.recipes.food.test;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class TestWebFilterChain implements WebFilterChain {

    private final String requiredRole;
    public TestWebFilterChain(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .switchIfEmpty(requiredRole != null ? Mono.error(new RuntimeException("Authentication is required.")) : Mono.empty())
                .map(authentication -> {
                    if (authentication.isAuthenticated() && requiredRole == null) {
                        return Mono.error(new RuntimeException("Authentication is not required."));
                    } else if (authentication.isAuthenticated() && requiredRole != null) {
                        if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority -> authority.equals(requiredRole))) {
                            return Mono.empty();
                        } else {
                            return Mono.error(new RuntimeException("Authentication is required but did not match."));
                        }
                    } else if (requiredRole == null) {
                        return Mono.empty();
                    } else {
                        return Mono.error(new RuntimeException("Authentication is required."));
                    }
                })
                .then();
    }
}
