package eus.ibai.family.recipes.food.test;

import eus.ibai.family.recipes.food.security.PathAuthorization;
import eus.ibai.family.recipes.food.security.PathAuthorizations;
import eus.ibai.family.recipes.food.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static org.springframework.http.HttpMethod.GET;

@Configuration
public class TestSecurityConfig {

    @Bean
    PathAuthorizations testPathAuthorizations() {
        List<PathAuthorization> pathAuthorizations = List.of(
                new PathAuthorization(GET, "/test/public/**"),
                new PathAuthorization(GET, "/test/protected", new String[]{Roles.FAMILY_MEMBER}),
                new PathAuthorization(GET, "/test/admin", new String[]{Roles.FAMILY_ADMIN})
        );
        return new PathAuthorizations(pathAuthorizations);
    }
}
