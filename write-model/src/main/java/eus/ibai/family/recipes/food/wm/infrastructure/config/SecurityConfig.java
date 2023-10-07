package eus.ibai.family.recipes.food.wm.infrastructure.config;

import eus.ibai.family.recipes.food.security.PathAuthorization;
import eus.ibai.family.recipes.food.security.PathAuthorizations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static eus.ibai.family.recipes.food.security.Roles.FAMILY_MEMBER;
import static eus.ibai.family.recipes.food.security.Roles.FOOD_COMMAND_NODE;
import static org.springframework.http.HttpMethod.*;

@Configuration
public class SecurityConfig {

    private static final String RECIPES_PATH_PATTERN = "/recipes/**";

    private static final String INGREDIENTS_PATH_PATTERN = "/ingredients/**";

    private static final String PROPERTIES_PATH_PATTERN = "/properties/**";

    @Bean
    PathAuthorizations pathAuthorizations() {
        List<PathAuthorization> pathAuthorizations = List.of(
                new PathAuthorization(POST, RECIPES_PATH_PATTERN, new String[]{FAMILY_MEMBER}),
                new PathAuthorization(PUT, RECIPES_PATH_PATTERN, new String[]{FAMILY_MEMBER}),
                new PathAuthorization(DELETE, RECIPES_PATH_PATTERN, new String[]{FAMILY_MEMBER}),
                new PathAuthorization(POST, INGREDIENTS_PATH_PATTERN, new String[]{FAMILY_MEMBER}),
                new PathAuthorization(PUT, INGREDIENTS_PATH_PATTERN, new String[]{FAMILY_MEMBER}),
                new PathAuthorization(DELETE, INGREDIENTS_PATH_PATTERN, new String[]{FAMILY_MEMBER}),
                new PathAuthorization(POST, PROPERTIES_PATH_PATTERN, new String[]{FAMILY_MEMBER}),
                new PathAuthorization(PUT, PROPERTIES_PATH_PATTERN, new String[]{FAMILY_MEMBER}),
                new PathAuthorization(DELETE, PROPERTIES_PATH_PATTERN, new String[]{FAMILY_MEMBER})
        );
        return new PathAuthorizations(pathAuthorizations);
    }

    @Bean
    @ConditionalOnProperty(name = "axon.distributed.enabled", havingValue = "true")
    PathAuthorizations distributedCommandBusPathAuthorizations() {
        List<PathAuthorization> pathAuthorizations = List.of(
                new PathAuthorization(GET, "/member-capabilities", new String[]{FOOD_COMMAND_NODE}),
                new PathAuthorization(POST, "/spring-command-bus-connector/command", new String[]{FOOD_COMMAND_NODE})
        );
        return new PathAuthorizations(pathAuthorizations);
    }
}
