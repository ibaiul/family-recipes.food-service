package eus.ibai.family.recipes.food.rm.infrastructure.config;

import eus.ibai.family.recipes.food.security.PathAuthorization;
import eus.ibai.family.recipes.food.security.PathAuthorizations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    PathAuthorizations distributedCommandBusPathAuthorizations() {
        List<PathAuthorization> pathAuthorizations = List.of(
                new PathAuthorization(HttpMethod.GET, "/recipes/**", new String[]{"FAMILY_MEMBER"}),
                new PathAuthorization(HttpMethod.GET, "/ingredients/**", new String[]{"FAMILY_MEMBER"}),
                new PathAuthorization(HttpMethod.GET, "/properties/**", new String[]{"FAMILY_MEMBER"}),
                new PathAuthorization(HttpMethod.GET, "/events/sse", new String[]{"FAMILY_MEMBER"})
        );
        return new PathAuthorizations(pathAuthorizations);
    }
}
