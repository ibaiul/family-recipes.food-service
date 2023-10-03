package eus.ibai.family.recipes.food.test;

import eus.ibai.family.recipes.food.security.PathAuthorization;
import eus.ibai.family.recipes.food.security.PathAuthorizations;
import eus.ibai.family.recipes.food.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.List;

@Configuration
public class TestSecurityConfig {

    @Bean
    PathAuthorizations testPathAuthorizations() {
        List<PathAuthorization> pathAuthorizations = List.of(
                new PathAuthorization(HttpMethod.GET, "/test/open/**"),
                new PathAuthorization(HttpMethod.GET, "/test/protected", new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(HttpMethod.GET, "/test/admin", new String[]{Roles.FAMILY_ADMIN})
        );
        return new PathAuthorizations(pathAuthorizations);
    }
}
