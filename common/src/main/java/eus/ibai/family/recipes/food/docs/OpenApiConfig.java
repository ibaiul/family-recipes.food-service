package eus.ibai.family.recipes.food.docs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI foodServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Food Service API")
                        .description("Service in charge of food entities part of the Family Recipes application.")
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerToken", List.of("read", "write")))
                .components(new Components()
                        .addSecuritySchemes("bearerToken", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)));
    }
}
