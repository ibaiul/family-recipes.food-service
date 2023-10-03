package eus.ibai.family.recipes.food.wm.infrastructure.config;

import eus.ibai.family.recipes.food.security.PathAuthorization;
import eus.ibai.family.recipes.food.security.PathAuthorizations;
import eus.ibai.family.recipes.food.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String RECIPES_PATH_PATTERN = "/recipes/**";

    private static final String INGREDIENTS_PATH_PATTERN = "/ingredients/**";

    private static final String PROPERTIES_PATH_PATTERN = "/properties/**";

    @Bean
    PathAuthorizations distributedCommandBusPathAuthorizations() {
        List<PathAuthorization> pathAuthorizations = List.of(
                new PathAuthorization(HttpMethod.POST, RECIPES_PATH_PATTERN, new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(HttpMethod.PUT, RECIPES_PATH_PATTERN, new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(HttpMethod.DELETE, RECIPES_PATH_PATTERN, new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(HttpMethod.POST, INGREDIENTS_PATH_PATTERN, new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(HttpMethod.PUT, INGREDIENTS_PATH_PATTERN, new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(HttpMethod.DELETE, INGREDIENTS_PATH_PATTERN, new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(HttpMethod.POST, PROPERTIES_PATH_PATTERN, new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(HttpMethod.PUT, PROPERTIES_PATH_PATTERN, new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(HttpMethod.DELETE, PROPERTIES_PATH_PATTERN, new String[]{Roles.FAMILY_MEMBER})
        );
        return new PathAuthorizations(pathAuthorizations);
    }
}
