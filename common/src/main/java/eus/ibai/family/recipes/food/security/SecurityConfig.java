package eus.ibai.family.recipes.food.security;

import eus.ibai.family.recipes.food.util.Temporary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final List<Tuple2<HttpMethod, String>> AUTH_ALLOW_LIST = List.of(
            Tuples.of(HttpMethod.GET, "/actuator/health"),
            Tuples.of(HttpMethod.GET, "/actuator/health/readiness"),
            Tuples.of(HttpMethod.GET, "/actuator/health/liveness"),
            Tuples.of(HttpMethod.GET, "/actuator/info"),
            Tuples.of(HttpMethod.POST, "/authentication/login"),
            Tuples.of(HttpMethod.POST, "/authentication/refresh"),
            Tuples.of(HttpMethod.GET, "/v3/api-docs/**"),
            Tuples.of(HttpMethod.GET, "/v3/api-docs.yaml"),
            Tuples.of(HttpMethod.GET, "/swagger-ui/**"),
            Tuples.of(HttpMethod.GET, "/swagger-ui.html")
    );

    @Bean
    SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http, JwtService jwtService, ReactiveAuthenticationManager authenticationManager) {
        JwtAuthorizationFilter jwtAuthorizationFilter = new JwtAuthorizationFilter(jwtService, AUTH_ALLOW_LIST);
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(it -> {
                            AUTH_ALLOW_LIST.forEach(pathMatcherTuple -> it.pathMatchers(pathMatcherTuple.getT1(), pathMatcherTuple.getT2()).permitAll());
                            it.anyExchange().authenticated(); // Default authenticated to avoid OWASP A01:2021 – Broken Access Control
                        }
                )
                .addFilterAt(jwtAuthorizationFilter, SecurityWebFiltersOrder.HTTP_BASIC)
                .build();
    }

    @Bean
    @Temporary("Will be re-implemented in a dedicated microservice including RBAC")
    public ReactiveAuthenticationManager reactiveAuthenticationManager(ReactiveUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        UserDetailsRepositoryReactiveAuthenticationManager authenticationManager = new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        authenticationManager.setPasswordEncoder(passwordEncoder);
        return authenticationManager;
    }

    @Bean
    @Temporary("Will be re-implemented in a dedicated microservice including RBAC")
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Temporary("Will be re-implemented in a dedicated microservice including RBAC")
    public MapReactiveUserDetailsService users(UserProperties userProperties) {
        List<UserDetails> inMemoryUsers = userProperties.getUsers().stream()
                .map(userDetails -> User.builder()
                        .username(userDetails.username())
                        .password(userDetails.password())
                        .roles(userDetails.roles().toArray(new String[0]))
                        .build())
                .toList();
        return new MapReactiveUserDetailsService(inMemoryUsers);
    }
}
