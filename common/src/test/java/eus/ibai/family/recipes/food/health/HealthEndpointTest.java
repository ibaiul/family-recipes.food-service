package eus.ibai.family.recipes.food.health;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.sql.DataSource;
import java.sql.SQLException;

import static eus.ibai.family.recipes.food.test.TestData.mockConnectionFactoryHealthUp;
import static eus.ibai.family.recipes.food.test.TestData.mockDatasourceHealthUp;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@ActiveProfiles("health")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthEndpointTest {

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean(name = "event-store-db")
    private DataSource dataSource;

    @Autowired
    private ApplicationContext applicationContext;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();
    }

    @Test
    void should_return_ok_when_application_healthy() throws SQLException {
        mockConnectionFactoryHealthUp(connectionFactory);
        mockDatasourceHealthUp(dataSource);

        await().atMost(3, SECONDS).untilAsserted(
                () -> webTestClient.get().uri("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .jsonPath("$.status").isEqualTo(UP)
                        .jsonPath("$.components.database.status").isEqualTo(UP)
                        .jsonPath("$.components.event-store.status").isEqualTo(UP));

    }

    @Test
    void should_return_service_unavailable_when_application_unhealthy() {
        mockConnectionFactoryHealthUp(connectionFactory);

        await().atMost(3, SECONDS).untilAsserted(
                () -> webTestClient.get().uri("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isEqualTo(SERVICE_UNAVAILABLE)
                        .expectBody()
                        .jsonPath("$.status").isEqualTo(DOWN)
                        .jsonPath("$.components.database.status").isEqualTo(UP)
                        .jsonPath("$.components.event-store.status").isEqualTo(DOWN));
    }

    @Test
    void should_return_ok_when_application_ready() throws SQLException {
        mockConnectionFactoryHealthUp(connectionFactory);
        mockDatasourceHealthUp(dataSource);

        await().atMost(3, SECONDS).untilAsserted(
                () -> webTestClient.get().uri("/actuator/health/readiness")
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isOk());
    }

    @Test
    void should_return_ok_when_application_live() throws SQLException {
        mockConnectionFactoryHealthUp(connectionFactory);
        mockDatasourceHealthUp(dataSource);

        await().atMost(3, SECONDS).untilAsserted(
                () -> webTestClient.get().uri("/actuator/health/liveness")
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isOk());
    }
}
