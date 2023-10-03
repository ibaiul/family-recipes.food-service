package eus.ibai.family.recipes.food.rm.infrastructure.config;

import eus.ibai.family.recipes.food.security.PathAuthorization;
import eus.ibai.family.recipes.food.security.PathAuthorizations;
import eus.ibai.family.recipes.food.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    PathAuthorizations pathAuthorizations() {
        List<PathAuthorization> pathAuthorizations = List.of(
                new PathAuthorization(HttpMethod.GET, "/recipes/**", new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(HttpMethod.GET, "/ingredients/**", new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(HttpMethod.GET, "/properties/**", new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(HttpMethod.GET, "/events/sse", new String[]{Roles.FAMILY_MEMBER})
        );
        return new PathAuthorizations(pathAuthorizations);
    }
}
