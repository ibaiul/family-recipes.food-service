package eus.ibai.family.recipes.food.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.ToDoubleFunction;

@Configuration(proxyBeanMethods = false)
public class HealthConfig {

    @Bean
    MeterRegistryCustomizer<MeterRegistry> overallHealthMetric(HealthEndpoint healthEndpoint) {
        return meterRegistry -> Gauge.builder("health", healthEndpoint, overallHealthValue())
                .tag("component", "aggregate")
                .strongReference(true)
                .register(meterRegistry);
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> healthRegistryCustomizer(HealthContributorRegistry healthContributorRegistry) {
        return meterRegistry -> healthContributorRegistry.stream()
                .forEach(namedContributor -> Gauge.builder("health", healthContributorRegistry, componentHealthValue(namedContributor.getName()))
                        .tag("component", namedContributor.getName().replace("State", ""))
                        .register(meterRegistry));
    }

    private ToDoubleFunction<HealthEndpoint> overallHealthValue() {
        return healthEndpoint -> statusToValue(healthEndpoint.health().getStatus());
    }

    private ToDoubleFunction<HealthContributorRegistry> componentHealthValue(String componentName) {
        return healthContributorRegistry -> {
            Status status = ((HealthIndicator) healthContributorRegistry.getContributor(componentName)).getHealth(false).getStatus();
            return statusToValue(status);
        };
    }

    private int statusToValue(Status status) {
        return Status.UP.equals(status) ? 1 : 0;
    }
}
