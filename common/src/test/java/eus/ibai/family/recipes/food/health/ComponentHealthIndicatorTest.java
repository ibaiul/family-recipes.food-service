package eus.ibai.family.recipes.food.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import reactor.test.StepVerifier;

import static eus.ibai.family.recipes.food.test.TestData.COMPONENT_NAME;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentHealthIndicatorTest {

    @Mock
    private HealthCache healthCache;

    private ComponentHealthIndicator componentHealthIndicator;

    @BeforeEach
    void beforeEach() {
        componentHealthIndicator = new ComponentHealthIndicator(COMPONENT_NAME, healthCache);
    }

    @Test
    void should_indicate_healthy_when_last_known_state_from_cache_is_healthy() {
        Health healthy = Health.up().build();
        when(healthCache.getHealth(COMPONENT_NAME)).thenReturn(healthy);

        StepVerifier.create(componentHealthIndicator.health())
                .expectNext(healthy)
                .verifyComplete();
    }

    @Test
    void should_indicate_unhealthy_when_last_known_state_from_cache_is_unhealthy() {
        Health unhealthy = Health.down().build();
        when(healthCache.getHealth(COMPONENT_NAME)).thenReturn(unhealthy);

        StepVerifier.create(componentHealthIndicator.health())
                .expectNext(unhealthy)
                .verifyComplete();
    }

    @Test
    void should_indicate_unhealthy_when_cannot_retrieve_last_known_state_from_cache() {
        when(healthCache.getHealth(COMPONENT_NAME)).thenReturn(null);

        StepVerifier.create(componentHealthIndicator.health())
                .expectNextMatches(health -> health.getStatus() == Status.DOWN)
                .verifyComplete();
    }
}