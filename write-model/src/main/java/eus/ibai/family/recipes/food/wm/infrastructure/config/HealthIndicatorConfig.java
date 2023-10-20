package eus.ibai.family.recipes.food.wm.infrastructure.config;

import eus.ibai.family.recipes.food.health.ComponentHealthIndicator;
import eus.ibai.family.recipes.food.health.HealthCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthIndicatorConfig {

    @Bean("service-registry")
    @ConditionalOnProperty(prefix = "management.health.service-registry", name = "enabled", havingValue = "true")
    public ComponentHealthIndicator serviceRegistryHealthIndicator(HealthCache healthCache) {
        return new ComponentHealthIndicator("service-registry", healthCache);
    }

    @Bean("command-bus")
    @ConditionalOnProperty(prefix = "management.health.command-bus", name = "enabled", havingValue = "true")
    public ComponentHealthIndicator commandBusHealthIndicator(HealthCache healthCache) {
        return new ComponentHealthIndicator("command-bus", healthCache);
    }
}
