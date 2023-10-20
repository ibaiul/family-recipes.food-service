package eus.ibai.family.recipes.food.wm.infrastructure.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class ServiceRegistryHealthContributorTest {

    @Mock
    private DiscoveryClient discoveryClient;

    private ServiceRegistryHealthContributor healthContributor;

    @BeforeEach
    void beforeEach() {
        healthContributor = new ServiceRegistryHealthContributor(discoveryClient, 60);
    }

    @Test
    void should_return_healthy_when_service_registry_available() {
        Health expectedHealth = Health.up().build();

        StepVerifier.create(healthContributor.doHealthCheck())
                .expectNext(expectedHealth)
                .verifyComplete();
    }

    @Test
    void should_return_unhealthy_when_service_registry_unavailable() {
        doThrow(new RuntimeException()).when(discoveryClient).probe();
        Health expectedHealth = Health.down().build();

        StepVerifier.create(healthContributor.doHealthCheck())
                .expectNext(expectedHealth)
                .verifyComplete();
    }
}
