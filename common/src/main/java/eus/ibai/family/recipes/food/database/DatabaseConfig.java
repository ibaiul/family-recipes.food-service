package eus.ibai.family.recipes.food.database;

import eus.ibai.family.recipes.food.health.ComponentHealthIndicator;
import eus.ibai.family.recipes.food.health.HealthCache;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class DatabaseConfig {

    private final HealthCache healthCache;

    @Bean("database")
    @ConditionalOnProperty(prefix = "management.health.database", name = "enabled", havingValue = "true")
    public ComponentHealthIndicator databaseHealthIndicator() {
        return new ComponentHealthIndicator("database", healthCache);
    }
}
