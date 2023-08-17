package eus.ibai.family.recipes.food.health;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.stream.Stream;

import static eus.ibai.family.recipes.food.test.TestData.mockConnectionFactoryHealthUp;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.actuate.health.Status.DOWN;
import static org.springframework.boot.actuate.health.Status.UP;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@ActiveProfiles("health")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = NONE)
class HealthMetricsTest {

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean(name = "event-store-db")
    private DataSource dataSource;

    @Autowired
    private MeterRegistry meterRegistry;

    @ParameterizedTest
    @MethodSource
    void should_record_component_health_metrics_when_application_is_running(String componentName, Status healthStatus) {
        mockConnectionFactoryHealthUp(connectionFactory);

        verifyComponentHealthRecorded(componentName, healthStatus);
    }

    private static Stream<Arguments> should_record_component_health_metrics_when_application_is_running() {
        return Stream.of(
            Arguments.of("database", UP),
            Arguments.of("event-store", DOWN),
            Arguments.of("liveness", UP),
            Arguments.of("readiness", UP)
        );
    }

    private void verifyComponentHealthRecorded(String componentName, Status status) {
        await().atMost(7, SECONDS).ignoreExceptions().untilAsserted(() -> {
            Gauge componentHealthGauge = meterRegistry.find("health")
                    .tag("component", componentName)
                    .gauge();
            assertThat(componentHealthGauge).isNotNull();
            assertThat(componentHealthGauge.value()).isEqualTo(status == UP ? 1.0d : 0.0d);
        });
    }
}