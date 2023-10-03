package eus.ibai.family.recipes.food.wm.infrastructure.config;

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
                new PathAuthorization(HttpMethod.POST, "/recipes/**", new String[]{"USER"}),
                new PathAuthorization(HttpMethod.PUT, "/recipes/**", new String[]{"USER"}),
                new PathAuthorization(HttpMethod.DELETE, "/recipes/**", new String[]{"USER"}),
                new PathAuthorization(HttpMethod.POST, "/ingredients/**", new String[]{"USER"}),
                new PathAuthorization(HttpMethod.PUT, "/ingredients/**", new String[]{"USER"}),
                new PathAuthorization(HttpMethod.DELETE, "/ingredients/**", new String[]{"USER"}),
                new PathAuthorization(HttpMethod.POST, "/properties/**", new String[]{"USER"}),
                new PathAuthorization(HttpMethod.PUT, "/properties/**", new String[]{"USER"}),
                new PathAuthorization(HttpMethod.DELETE, "/properties/**", new String[]{"USER"})
        );
        return new PathAuthorizations(pathAuthorizations);
    }
}
