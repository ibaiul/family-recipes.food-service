package eus.ibai.family.recipes.food.wm.infrastructure.health;

import eus.ibai.family.recipes.food.wm.infrastructure.zookeeper.ZookeeperContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
class ServiceRegistryHealthIT {

    private static final ZookeeperContainer<?> zookeeperContainer = new ZookeeperContainer<>("bitnami/zookeeper")
            .withReuse(true);

    static {
        zookeeperContainer.start();
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void should_return_service_registry_health_status() {
        await().atMost(3, SECONDS).untilAsserted(
                () -> webTestClient.get().uri("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON)
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .jsonPath("$.status").isEqualTo("UP")
                        .jsonPath("$.components.service-registry.status").isEqualTo("UP")
                        .jsonPath("$.components.zookeeper").doesNotExist()
                        .jsonPath("$.components.refreshScope").doesNotExist()
                        .jsonPath("$.components.discoveryComposite").doesNotExist()
                        .jsonPath("$.components.reactiveDiscoveryClients").doesNotExist());
    }

    @DynamicPropertySource
    private static void setDynamicProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.zookeeper.connect-string", () -> "localhost:" + zookeeperContainer.getHttpPort());
    }
}
