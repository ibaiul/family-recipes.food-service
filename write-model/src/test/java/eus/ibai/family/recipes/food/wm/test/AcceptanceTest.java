package eus.ibai.family.recipes.food.wm.test;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static eus.ibai.family.recipes.food.test.TestUtils.stubNewRelicSendMetricResponse;
import static java.lang.String.format;

@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTest {

    private static final int POSTGRES_PORT = 5432;

    @Container
    private static final PostgreSQLContainer<?> postgreSqlContainer = new PostgreSQLContainer<>("postgres:13.8")
            .withDatabaseName("acceptance-test-db")
            .withUsername("sa")
            .withPassword("sa")
            .withReuse(true);

    @RegisterExtension
    private static final WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .configureStaticDsl(true)
            .build();

    @BeforeEach
    void beforeEach() {
        stubNewRelicSendMetricResponse();
    }

    @DynamicPropertySource
    public static void setDynamicProperties(final DynamicPropertyRegistry registry) {
        log.debug("Setting dynamic properties.");
        registry.add("spring.datasource.url", postgreSqlContainer::getJdbcUrl);
        registry.add("spring.datasource.driverClassName", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.url", postgreSqlContainer::getJdbcUrl);
        registry.add("newrelic.enabled", () -> "true");
        registry.add("newrelic.metrics.ingest-uri", () -> format("%s/metric/v1", wiremock.baseUrl()));
    }
}
