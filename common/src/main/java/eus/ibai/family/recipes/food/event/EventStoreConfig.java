package eus.ibai.family.recipes.food.event;

import eus.ibai.family.recipes.food.health.ComponentHealthIndicator;
import eus.ibai.family.recipes.food.health.HealthCache;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class EventStoreConfig {

    private final HealthCache healthCache;

    @Bean("event-store")
    @ConditionalOnProperty(prefix = "management.health.event-store", name = "enabled", havingValue = "true")
    public ComponentHealthIndicator eventStoreHealthIndicator() {
        return new ComponentHealthIndicator("event-store", healthCache);
    }
}
