package eus.ibai.family.recipes.food.wm.infrastructure.health;

import eus.ibai.family.recipes.food.health.AbstractComponentHealthContributor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

import static org.springframework.cloud.zookeeper.support.StatusConstants.INSTANCE_STATUS_KEY;
import static org.springframework.cloud.zookeeper.support.StatusConstants.STATUS_UP;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "management.health.command-bus", name = "enabled", havingValue = "true")
public class CommandBusHealthContributor extends AbstractComponentHealthContributor {

    private final DiscoveryClient discoveryClient;

    private final String serviceName;

    public CommandBusHealthContributor(DiscoveryClient discoveryClient, @Value("${spring.application.name}") String serviceName,
                                       @Value("${management.health.command-bus.interval:300}") long interval) {
        super("command-bus", interval);
        this.discoveryClient = discoveryClient;
        this.serviceName = serviceName;
    }

    @Override
    public Mono<Health> doHealthCheck() {
        if (discoveryClient == null) {
            return Mono.just(Health.up().withDetail("mode", "local").build());
        }

        Health.Builder health = new Health.Builder().withDetail("mode", "distributed");
        List<ServiceInstance> registeredInstances;
        try {
            registeredInstances = discoveryClient.getServices().stream()
                    .filter(registeredServiceName -> registeredServiceName.equals(serviceName))
                    .map(discoveryClient::getInstances)
                    .flatMap(Collection::stream)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to retrieve status of registered instances.");
            return Mono.just(health.down().build());
        }
        if (registeredInstances.isEmpty()) {
            return Mono.just(health.unknown().build());
        }
        // If some instances are out of service, their statuses should have propagated to all active instances and the ConsistentHash should be up-to-date.
        // However, unless we compare the members in the ConsistentHash with the service instances that are UP we will not be able to determine the health with %100 accuracy.
        return allInstancesUp(registeredInstances) ? Mono.just(health.up().build()) : Mono.just(health.unknown().build());
    }

    private boolean allInstancesUp(List<ServiceInstance> registeredInstances) {
        return registeredInstances.stream()
                .allMatch(this::isInstanceUp);
    }

    private boolean isInstanceUp(ServiceInstance serviceInstance) {
        if (serviceInstance.getMetadata() == null) {
            return false;
        }
        return serviceInstance.getMetadata().entrySet().stream()
                .anyMatch(metadataEntry -> INSTANCE_STATUS_KEY.equals(metadataEntry.getKey()) && STATUS_UP.equals(metadataEntry.getValue()));
    }
}
