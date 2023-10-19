package eus.ibai.family.recipes.food.wm.infrastructure.health;

import eus.ibai.family.recipes.food.health.ComponentHealthContributor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "management.health.service-registry", name = "enabled", havingValue = "true")
public class ServiceRegistryHealthContributor implements ComponentHealthContributor {

    private static final String COMPONENT_NAME = "serviceRegistry";

    private final DiscoveryClient discoveryClient;

    private final long interval;

    public ServiceRegistryHealthContributor(DiscoveryClient discoveryClient, @Value("${management.health.service-registry.interval:300}") long interval) {
        this.discoveryClient = discoveryClient;
        this.interval = interval;
    }

    @Override
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    @Override
    public Mono<Health> doHealthCheck() {
        try {
            discoveryClient.probe();
            return Mono.just(Health.up().build());
        } catch (Exception e) {
            log.error("Service registry health failed.", e);
            return Mono.just(Health.down().build());
        }
    }

    @Override
    public long getInterval() {
        return interval;
    }
}
