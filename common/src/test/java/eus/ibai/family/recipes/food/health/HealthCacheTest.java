package eus.ibai.family.recipes.food.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;

import static eus.ibai.family.recipes.food.test.TestData.COMPONENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class HealthCacheTest {

    private HealthCache healthCache;

    @BeforeEach
    void beforeEach() {
        healthCache = new HealthCache();
    }

    @Test
    void should_return_health_of_component() {
        Health expectedHealth = Health.up().build();

        healthCache.setHealth(COMPONENT_NAME, expectedHealth);

        Health actualHealth = healthCache.getHealth(COMPONENT_NAME);
        assertThat(actualHealth).isEqualTo(expectedHealth);
    }

    @Test
    void should_return_unknown_when_getting_health_of_unregistered_component() {
        Health health = healthCache.getHealth("notRegisteredComponent");

        assertThat(health).isEqualTo(Health.unknown().build());
    }
}