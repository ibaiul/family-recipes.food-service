package eus.ibai.family.recipes.food.wm.test;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static eus.ibai.family.recipes.food.test.TestUtils.stubNewRelicSendMetricResponse;
import static java.lang.String.format;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AcceptanceTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSqlContainer = new PostgreSQLContainer<>("postgres:13.8")
            .withDatabaseName("acceptance-test-db")
            .withUsername("sa")
            .withPassword("sa")
            .withReuse(true);

    @Container
    private static final LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.2.0"))
            .withServices(S3);

    @RegisterExtension
    private static final WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .configureStaticDsl(true)
            .build();

    @Autowired
    private S3AsyncClient s3Client;

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
        registry.add("s3.endpoint", localstack::getEndpoint);
        registry.add("s3.region", localstack::getRegion);
        registry.add("s3.accessKey", localstack::getAccessKey);
        registry.add("s3.secretKey", localstack::getSecretKey);
        registry.add("s3.bucket", () -> "family-recipes.food-service");
        registry.add("newrelic.enabled", () -> "true");
        registry.add("newrelic.metrics.ingest-uri", () -> format("%s/metric/v1", wiremock.baseUrl()));
    }

    protected void createS3Bucket() throws ExecutionException, InterruptedException {
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket("family-recipes.food-service")
                .acl(BucketCannedACL.PRIVATE)
                .build();
        Mono.fromCompletionStage(s3Client.createBucket(createBucketRequest))
                .as(StepVerifier::create)
                .expectNextMatches(response -> response.sdkHttpResponse().isSuccessful())
                .verifyComplete();
        log.info("List of S3 buckets: {}", s3Client.listBuckets().get().buckets());
    }
}
