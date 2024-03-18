package eus.ibai.family.recipes.food.rm.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "recipes")
public class RecipeProperties {

    private ImageProperties images;

    public record ImageProperties(String storagePath) {}
}
