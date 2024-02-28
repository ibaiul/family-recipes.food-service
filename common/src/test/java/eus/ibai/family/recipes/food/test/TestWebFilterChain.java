package eus.ibai.family.recipes.food.test;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class TestWebFilterChain implements WebFilterChain {

    private String authenticatedRole;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .doOnNext(authentication -> {
                    if (authentication.isAuthenticated()) {
                        authenticatedRole = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse(null);
                    }
                })
                .then();
    }

    public String getAuthenticatedRole() {
        return authenticatedRole;
    }
}
