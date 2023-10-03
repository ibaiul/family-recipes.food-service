package eus.ibai.family.recipes.food.security;

import eus.ibai.family.recipes.food.util.Temporary;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class GlobalSecurityConfig {

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
    SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http, JwtService jwtService, List<PathAuthorizations> pathAuthorizations) {
        List<Tuple2<HttpMethod, String>> aggregatedAuthAllowList = pathAuthorizations.stream()
                .map(PathAuthorizations::paths)
                .flatMap(List::stream)
                .filter(pathAuthorization -> pathAuthorization.requiredRoles() == null)
                .map(pathAuthorization -> Tuples.of(pathAuthorization.httpMethod(), pathAuthorization.pathPattern()))
                .toList();
        JwtAuthorizationFilter jwtAuthorizationFilter = new JwtAuthorizationFilter(jwtService, aggregatedAuthAllowList);
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
        List<PathAuthorization> pathAuthorizationList = AUTH_ALLOW_LIST.stream()
                .map(openPath -> new PathAuthorization(openPath.getT1(), openPath.getT2()))
                .toList();
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
