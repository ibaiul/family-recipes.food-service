package eus.ibai.family.recipes.food.rm.test;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static eus.ibai.family.recipes.food.test.TestUtils.stubNewRelicSendMetricResponse;
import static java.lang.String.format;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTest {

    private static final int POSTGRES_PORT = 5432;

    private static final PostgreSQLContainer<?> postgreSqlContainer = new PostgreSQLContainer<>("postgres:13.8")
            .withUsername("sa")
            .withPassword("sa")
            .withInitScript("db/acceptance-test-db-init-script.sql")
            .withReuse(true);

    static {
        postgreSqlContainer.start();
        log.debug("Started PostgreSQL container on port {}", postgreSqlContainer.getMappedPort(POSTGRES_PORT));
    }

    @RegisterExtension
    private static final WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .configureStaticDsl(true)
            .build();

    @Autowired
    protected RecipeEntityRepository recipeEntityRepository;

    @Autowired
    protected RecipeIngredientEntityRepository recipeIngredientEntityRepository;

    @Autowired
    protected IngredientEntityRepository ingredientEntityRepository;

    @Autowired
    protected IngredientPropertyEntityRepository ingredientPropertyEntityRepository;

    @Autowired
    protected PropertyEntityRepository propertyEntityRepository;

    @BeforeEach
    void beforeEach() {
        stubNewRelicSendMetricResponse();
    }

    @AfterEach
    void afterEach() {
        ingredientPropertyEntityRepository.deleteAll().as(StepVerifier::create).verifyComplete();
        recipeIngredientEntityRepository.deleteAll().as(StepVerifier::create).verifyComplete();
        propertyEntityRepository.deleteAll().as(StepVerifier::create).verifyComplete();
        ingredientEntityRepository.deleteAll().as(StepVerifier::create).verifyComplete();
        recipeEntityRepository.deleteAll().as(StepVerifier::create).verifyComplete();
    }

    @DynamicPropertySource
    public static void setDatasourceProperties(final DynamicPropertyRegistry registry) {
        log.debug("Setting dynamic properties.");
        String defaultDatabaseName = postgreSqlContainer.getDatabaseName();
        registry.add("spring.datasource.url", () -> postgreSqlContainer.getJdbcUrl().replace(defaultDatabaseName, "acceptance-command-db"));
        registry.add("spring.datasource.driverClassName", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.r2dbc.url", () -> postgreSqlContainer.getJdbcUrl().replace(defaultDatabaseName, "acceptance-query-db").replace("jdbc", "r2dbc"));
        registry.add("spring.flyway.url", () -> postgreSqlContainer.getJdbcUrl().replace(defaultDatabaseName, "acceptance-query-db"));
        registry.add("newrelic.enabled", () -> "true");
        registry.add("newrelic.metrics.ingest-uri", () -> format("%s/metric/v1", wiremock.baseUrl()));
    }
}
