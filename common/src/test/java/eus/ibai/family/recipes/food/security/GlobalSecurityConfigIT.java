package eus.ibai.family.recipes.food.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
class GlobalSecurityConfigIT {

    @Autowired
    private SecurityWebFilterChain securityWebFilterChain;

    @Test
    void should_allow_list_public_endpoints() {
        List<Tuple2<HttpMethod, String>> expectedAllowedPublicPaths = List.of(
                Tuples.of(HttpMethod.GET, "/test/public/**"),
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

        securityWebFilterChain.getWebFilters()
                .filter(webFilter -> webFilter instanceof JwtAuthorizationFilter)
                .cast(JwtAuthorizationFilter.class)
                .as(StepVerifier::create)
                .assertNext(authFilter -> assertThat(authFilter.getPublicPaths()).isEqualTo(expectedAllowedPublicPaths))
                .verifyComplete();
    }
}
