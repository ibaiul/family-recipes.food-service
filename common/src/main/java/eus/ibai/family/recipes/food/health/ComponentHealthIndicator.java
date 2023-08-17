package eus.ibai.family.recipes.food.health;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import reactor.core.publisher.Mono;

public class ComponentHealthIndicator extends AbstractReactiveHealthIndicator {

    private final String componentName;

    private final HealthCache healthCache;

    public ComponentHealthIndicator(String componentName, HealthCache healthCache) {
        super(componentName + " is unhealthy.");
        this.componentName = componentName;
        this.healthCache = healthCache;
    }

    @Override
    protected Mono<Health> doHealthCheck(Health.Builder builder) {
        return Mono.just(healthCache.getHealth(componentName));
    }
}
