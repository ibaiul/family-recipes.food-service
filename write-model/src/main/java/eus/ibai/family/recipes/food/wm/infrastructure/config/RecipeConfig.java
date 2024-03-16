package eus.ibai.family.recipes.food.wm.infrastructure.config;

import eus.ibai.family.recipes.food.wm.domain.recipe.RecipeProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecipeConfig {

    @Bean
    @ConfigurationProperties(prefix = "recipes")
    public RecipeProperties recipeProperties() {
        return RecipeProperties.builder().build();
    }
}
