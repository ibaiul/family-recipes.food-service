package eus.ibai.family.recipes.food.security;

import eus.ibai.family.recipes.food.util.Temporary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.AuthorizeExchangeSpec;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Configuration
@EnableWebFluxSecurity
public class GlobalSecurityConfig {

    @Bean
    SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http, JwtService jwtService, List<PathAuthorizations> pathAuthorizations) {
        List<PathAuthorization> publicPaths = pathAuthorizations.stream()
                .map(PathAuthorizations::paths)
                .flatMap(List::stream)
                .filter(pathAuthorization -> pathAuthorization.requiredRoles() == null)
                .toList();
        JwtAuthorizationFilter jwtAuthorizationFilter = new JwtAuthorizationFilter(jwtService, publicPaths);
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(it -> setUpPathAuthorizations(it, pathAuthorizations))
                .addFilterAt(jwtAuthorizationFilter, SecurityWebFiltersOrder.HTTP_BASIC)
                .build();
    }

    @Bean
    PathAuthorizations commonOpenPaths() {
        List<PathAuthorization> pathAuthorizationList = List.of(
                new PathAuthorization(GET, "/actuator/health"),
                new PathAuthorization(GET, "/actuator/health/readiness"),
                new PathAuthorization(GET, "/actuator/health/liveness"),
                new PathAuthorization(GET, "/actuator/info"),
                new PathAuthorization(POST, "/authentication/login"),
                new PathAuthorization(POST, "/authentication/refresh"),
                new PathAuthorization(GET, "/v3/api-docs/**"),
                new PathAuthorization(GET, "/v3/api-docs.yaml"),
                new PathAuthorization(GET, "/swagger-ui/**"),
                new PathAuthorization(GET, "/swagger-ui.html")
        );
        return new PathAuthorizations(pathAuthorizationList);
    }

    private void setUpPathAuthorizations(AuthorizeExchangeSpec authorizeExchangeSpec, List<PathAuthorizations> pathAuthorizations) {
        pathAuthorizations.stream()
                .map(PathAuthorizations::paths)
                .flatMap(List::stream)
                .forEach(pathAuthorization -> {
                    if (pathAuthorization.requiredRoles() != null) {
                        authorizeExchangeSpec.pathMatchers(pathAuthorization.httpMethod(), pathAuthorization.pathPattern()).hasAnyRole(pathAuthorization.requiredRoles());
                    } else {
                        authorizeExchangeSpec.pathMatchers(pathAuthorization.httpMethod(), pathAuthorization.pathPattern()).permitAll();
                    }
                });
        authorizeExchangeSpec.anyExchange().denyAll();
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
    public MapReactiveUserDetailsService users(UserProperties userProperties, ServiceProperties serviceProperties, PasswordEncoder passwordEncoder) {
        Stream<UserDetails> inMemoryUsers = userProperties.getUsers().stream()
                .map(userDetails -> User.builder()
                        .username(userDetails.username())
                        .password(userDetails.password())
                        .roles(userDetails.roles().toArray(new String[0]))
                        .build());
        Stream<UserDetails> inMemoryServices = Optional.ofNullable(serviceProperties.getServices())
                .orElseGet(Collections::emptyList)
                .stream()
                .map(serviceDetails -> User.builder()
                        .username(serviceDetails.serviceName())
                        .password(UUID.randomUUID().toString())
                        .passwordEncoder(passwordEncoder::encode)
                        .roles(serviceDetails.roles().toArray(new String[0]))
                        .accountLocked(true)
                        .build());
        List<UserDetails> inMemoryAccounts = Stream.concat(inMemoryUsers, inMemoryServices).toList();
        return new MapReactiveUserDetailsService(inMemoryAccounts);
    }
}
