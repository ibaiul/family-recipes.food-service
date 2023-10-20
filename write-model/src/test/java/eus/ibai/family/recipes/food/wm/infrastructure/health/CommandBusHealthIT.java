package eus.ibai.family.recipes.food.wm.infrastructure.health;

import eus.ibai.family.recipes.food.wm.infrastructure.zookeeper.ZookeeperContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

@ActiveProfiles("axon-distributed")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommandBusHealthIT {

    private static final ZookeeperContainer<?> zookeeperContainer = new ZookeeperContainer<>("bitnami/zookeeper")
            .withReuse(true);

    static {
        zookeeperContainer.start();
    }

    @Autowired
    private WebTestClient webTestClient;

    @Value("${management.health.command-bus.interval}")
    private long healthCheckInterval;

    @Test
    void should_return_command_bus_health_status() {
        await().atMost(healthCheckInterval * 3, SECONDS).untilAsserted(
                () -> webTestClient.get().uri("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .jsonPath("$.status").isEqualTo("UP")
                        .jsonPath("$.components.command-bus.status").isEqualTo("UP"));
    }

    @DynamicPropertySource
    private static void setDynamicProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.zookeeper.connect-string", () -> "localhost:" + zookeeperContainer.getHttpPort());
    }
}
