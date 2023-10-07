package eus.ibai.family.recipes.food.rm.infrastructure.config;

import eus.ibai.family.recipes.food.security.PathAuthorization;
import eus.ibai.family.recipes.food.security.PathAuthorizations;
import eus.ibai.family.recipes.food.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.springframework.http.HttpMethod.GET;

@Configuration
public class SecurityConfig {

    @Bean
    PathAuthorizations pathAuthorizations() {
        List<PathAuthorization> pathAuthorizations = List.of(
                new PathAuthorization(GET, "/recipes/**", new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(GET, "/ingredients/**", new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(GET, "/properties/**", new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(GET, "/events/sse", new String[]{Roles.FAMILY_MEMBER})
        );
        return new PathAuthorizations(pathAuthorizations);
    }
}
