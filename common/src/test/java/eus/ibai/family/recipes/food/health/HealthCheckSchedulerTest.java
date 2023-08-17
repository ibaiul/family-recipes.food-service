package eus.ibai.family.recipes.food.health;

import eus.ibai.family.recipes.food.database.DatabaseHealthContributor;
import eus.ibai.family.recipes.food.event.EventStoreHealthContributor;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import java.util.List;

import static eus.ibai.family.recipes.food.test.TestData.COMPONENT_NAME;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthCheckSchedulerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private ConnectionFactory connectionFactory;

    private HealthCache healthCache;

    private HealthCheckScheduler healthCheckScheduler;

    @BeforeEach
    void beforeEach() {
        healthCache = new HealthCache();
        EventStoreHealthContributor eventStoreHealthContributor = new EventStoreHealthContributor(dataSource, 1);
        DatabaseHealthContributor databaseHealthContributor = new DatabaseHealthContributor(connectionFactory, 1);
        healthCheckScheduler = new HealthCheckScheduler(healthCache, List.of(eventStoreHealthContributor, databaseHealthContributor), 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"event-store", "database"})
    void should_return_all_components_healthy_when_application_starts(String componentName) {
        Health expectedHealth = Health.up().build();

        healthCheckScheduler.init();

        Health componentHealth = healthCache.getHealth(componentName);
        assertThat(componentHealth).isEqualTo(expectedHealth);
    }

    @Test
    void should_update_health_cache_when_checking_database_health_periodically() {
        healthCheckScheduler.init();
        Health expectedHealth = Health.down().build();

        await().atMost(3, SECONDS).untilAsserted(() -> {
            Health componentHealth = healthCache.getHealth("database");
            assertThat(componentHealth).isEqualTo(expectedHealth);
        });
    }

    @Test
    void should_update_health_cache_when_checking_event_store_health_periodically() {
        healthCheckScheduler.init();
        Health expectedHealth = Health.down().build();

        await().atMost(3, SECONDS).untilAsserted(() -> {
            Health componentHealth = healthCache.getHealth("event-store");
            assertThat(componentHealth).isEqualTo(expectedHealth);
        });
    }

    @Test
    void should_schedule_component_healthcheck_at_intervals() {
        long interval = 1;
        ComponentHealthContributor componentHealthContributor = mock(ComponentHealthContributor.class);
        when(componentHealthContributor.getComponentName()).thenReturn(COMPONENT_NAME);
        when(componentHealthContributor.doHealthCheck()).thenReturn(Mono.just(Health.up().build()));
        when(componentHealthContributor.getInterval()).thenReturn(interval);
        healthCheckScheduler = new HealthCheckScheduler(healthCache, List.of(componentHealthContributor), -1);
        int expectedScheduledHealthcheckAmount = 3;

        healthCheckScheduler.init();

        await().atMost((expectedScheduledHealthcheckAmount * interval) + 1, SECONDS)
                .untilAsserted(() -> verify(componentHealthContributor, times(expectedScheduledHealthcheckAmount)).doHealthCheck());
    }
}
