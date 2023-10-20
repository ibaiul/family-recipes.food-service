package eus.ibai.family.recipes.food.wm.infrastructure.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.zookeeper.support.StatusConstants.INSTANCE_STATUS_KEY;
import static org.springframework.cloud.zookeeper.support.StatusConstants.STATUS_UP;

@ExtendWith(MockitoExtension.class)
class CommandBusHealthContributorTest {

    private static final String SERVICE_NAME = "serviceName";

    @Mock
    private DiscoveryClient discoveryClient;

    private CommandBusHealthContributor healthContributor;

    @BeforeEach
    void beforeEach() {
        ObjectProvider<DiscoveryClient> discoveryClientProvider = new SimpleObjectProvider<>(discoveryClient);
        healthContributor = new CommandBusHealthContributor(discoveryClientProvider, SERVICE_NAME, 60);
    }

    @Test
    void should_return_healthy_when_running_in_local_mode() {
        ObjectProvider<DiscoveryClient> discoveryClientProvider = new SimpleObjectProvider<>(null);
        healthContributor = new CommandBusHealthContributor(discoveryClientProvider, SERVICE_NAME, 60);
        Health expectedHealth = Health.up().withDetail("mode", "local").build();

        StepVerifier.create(healthContributor.doHealthCheck())
                .expectNext(expectedHealth)
                .verifyComplete();
    }

    @Test
    void should_return_healthy_when_running_in_distributed_mode_and_all_registered_instances_are_up() {
        when(discoveryClient.getServices()).thenReturn(List.of(SERVICE_NAME, "otherService"));
        ServiceInstance serviceInstance1 = mock(ServiceInstance.class);
        when(serviceInstance1.getMetadata()).thenReturn(Map.of(INSTANCE_STATUS_KEY, STATUS_UP));
        ServiceInstance serviceInstance2 = mock(ServiceInstance.class);
        when(serviceInstance2.getMetadata()).thenReturn(Map.of(INSTANCE_STATUS_KEY, STATUS_UP));
        when(discoveryClient.getInstances(SERVICE_NAME)).thenReturn(List.of(serviceInstance1, serviceInstance2));
        Health expectedHealth = Health.up().withDetail("mode", "distributed").build();

        StepVerifier.create(healthContributor.doHealthCheck())
                .expectNext(expectedHealth)
                .verifyComplete();
    }

    @Test
    void should_return_unknown_when_running_in_distributed_mode_and_not_all_registered_instances_are_up() {
        when(discoveryClient.getServices()).thenReturn(List.of(SERVICE_NAME, "otherService"));
        ServiceInstance serviceInstance1 = mock(ServiceInstance.class);
        when(serviceInstance1.getMetadata()).thenReturn(Map.of(INSTANCE_STATUS_KEY, STATUS_UP));
        ServiceInstance serviceInstance2 = mock(ServiceInstance.class);
        when(serviceInstance2.getMetadata()).thenReturn(null);
        when(discoveryClient.getInstances(SERVICE_NAME)).thenReturn(List.of(serviceInstance1, serviceInstance2));
        Health expectedHealth = Health.unknown().withDetail("mode", "distributed").build();

        StepVerifier.create(healthContributor.doHealthCheck())
                .expectNext(expectedHealth)
                .verifyComplete();
    }

    @Test
    void should_return_unknown_when_running_in_distributed_mode_and_service_is_not_registered_yet() {
        when(discoveryClient.getServices()).thenReturn(List.of("otherService"));
        Health expectedHealth = Health.unknown().withDetail("mode", "distributed").build();

        StepVerifier.create(healthContributor.doHealthCheck())
                .expectNext(expectedHealth)
                .verifyComplete();
    }

    @Test
    void should_return_unknown_when_running_in_distributed_mode_and_no_registered_instances_are_found() {
        when(discoveryClient.getServices()).thenReturn(List.of(SERVICE_NAME, "otherService"));
        when(discoveryClient.getInstances(SERVICE_NAME)).thenReturn(Collections.emptyList());
        Health expectedHealth = Health.unknown().withDetail("mode", "distributed").build();

        StepVerifier.create(healthContributor.doHealthCheck())
                .expectNext(expectedHealth)
                .verifyComplete();
    }

    @Test
    void should_return_unhealthy_when_running_in_distributed_mode_and_service_registry_connection_fails() {
        when(discoveryClient.getServices()).thenThrow(new RuntimeException());
        Health expectedHealth = Health.down().withDetail("mode", "distributed").build();

        StepVerifier.create(healthContributor.doHealthCheck())
                .expectNext(expectedHealth)
                .verifyComplete();
    }
}
