package eus.ibai.family.recipes.food.wm.domain.file;

import eus.ibai.family.recipes.food.wm.infrastructure.config.AwsConfig;
import eus.ibai.family.recipes.food.wm.infrastructure.file.S3FileStorage;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Testcontainers
class FileStorageIT {

    @Container
    private static final LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.2.0"))
            .withServices(S3);

    private MeterRegistry meterRegistry;

    private S3AsyncClient s3Client;

    private FileStorage fileStorage;

    @BeforeEach
    void beforeEach() throws URISyntaxException {
        meterRegistry = new SimpleMeterRegistry();
        s3Client = spy(new AwsConfig().s3client(localstack.getEndpoint().toASCIIString(), localstack.getRegion(), localstack.getAccessKey(), localstack.getSecretKey()));
        fileStorage = new S3FileStorage(s3Client, "bucket", meterRegistry);
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket("bucket")
                .acl(BucketCannedACL.PRIVATE)
                .build();
        Mono.fromCompletionStage(s3Client.createBucket(createBucketRequest))
                .as(StepVerifier::create)
                .expectNextMatches(response -> response.sdkHttpResponse().isSuccessful())
                .verifyComplete();
    }

    @Test
    void should_store_file() throws IOException {
        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/images/albondigas.png"));
        ByteBuffer fileContent = ByteBuffer.wrap(data);
        Map<String, String> fileMetadata = Map.of("entity", "recipe", "entityid", "entityId");

        AtomicReference<String> fileId = new AtomicReference<>();
        fileStorage.storeFile("recipes/images/", IMAGE_PNG_VALUE, data.length, Flux.just(fileContent), fileMetadata)
                .as(StepVerifier::create)
                .consumeNextWith(fileId::set)
                .verifyComplete();

        retrieveFile(fileId.get())
                .as(StepVerifier::create)
                .assertNext(file -> {
                    assertThat(file.fileName()).isEqualTo(fileId.get());
                    assertThat(file.contentType()).isEqualTo("image/png");
                    assertThat(file.contentLength()).isEqualTo(data.length);
                    assertThat(file.metadata()).containsExactlyInAnyOrderEntriesOf(fileMetadata);
                })
                .verifyComplete();

        verifyUploadSucceededMetricRecorded(1);
    }

    @ParameterizedTest
    @MethodSource
    void should_map_exception_when_storing_file_fails(CompletableFuture<PutObjectResponse> s3Response) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/images/albondigas.png"));
        ByteBuffer fileContent = ByteBuffer.wrap(data);
        Map<String, String> fileMetadata = Map.of("entity", "recipe", "entityid", "entityId");
        doReturn(s3Response).when(s3Client).putObject(any(PutObjectRequest.class), any(AsyncRequestBody.class));

        fileStorage.storeFile("recipes/images/", IMAGE_PNG_VALUE, data.length, Flux.just(fileContent), fileMetadata)
                .as(StepVerifier::create)
                .verifyError(IOException.class);

        verifyUploadFailedMetricRecorded(1);
    }

    @Test
    void should_delete_file() {
        String imageId = storeFile();

        fileStorage.deleteFile("recipes/images/" + imageId)
                .as(StepVerifier::create)
                .verifyComplete();

        verifyDeleteSucceededMetricRecorded(1);
    }

    @ParameterizedTest
    @MethodSource
    void should_map_exception_when_deleting_file_fails(CompletableFuture<DeleteObjectResponse> s3Response) {
        doReturn(s3Response).when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        fileStorage.deleteFile("recipes/images/imageId")
                .as(StepVerifier::create)
                .verifyError(IOException.class);

        verifyDeleteFailedMetricRecorded(1);
    }

    static Stream<Arguments> should_map_exception_when_storing_file_fails() {
        return Stream.of(
            Arguments.of(CompletableFuture.failedFuture(new Throwable(""))),
            Arguments.of(CompletableFuture.completedFuture(PutObjectResponse.builder().sdkHttpResponse(SdkHttpResponse.builder().statusCode(500).build()).build()))
        );
    }

    static Stream<Arguments> should_map_exception_when_deleting_file_fails() {
        return Stream.of(
                Arguments.of(CompletableFuture.failedFuture(new Throwable(""))),
                Arguments.of(CompletableFuture.completedFuture(DeleteObjectResponse.builder().sdkHttpResponse(SdkHttpResponse.builder().statusCode(500).build()).build()))
        );
    }

    private void verifyUploadSucceededMetricRecorded(int expected) {
        verifyS3MetricRecorded("upload", "succeeded", expected);
    }

    private void verifyUploadFailedMetricRecorded(int expected) {
        verifyS3MetricRecorded("upload", "failed", expected);
    }

    private void verifyDeleteSucceededMetricRecorded(int expected) {
        verifyS3MetricRecorded("delete", "succeeded", expected);
    }

    private void verifyDeleteFailedMetricRecorded(int expected) {
        verifyS3MetricRecorded("delete", "failed", expected);
    }

    private void verifyS3MetricRecorded(String action, String status, int expected) {
        await().atMost(1, SECONDS).untilAsserted(() -> {
            DistributionSummary metric = meterRegistry.find("s3." + action)
                    .tag("status", status)
                    .summary();
            assertThat(metric).isNotNull();
            assertThat(metric.count()).isEqualTo(expected);
        });
    }

    public Mono<File> retrieveFile(String fileKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket("bucket")
                .key("recipes/images/" + fileKey)
                .build();

        return Mono.fromFuture(s3Client.getObject(request, AsyncResponseTransformer.toBytes()))
                .flatMap(response -> {
                    if (response.response().sdkHttpResponse() == null || !response.response().sdkHttpResponse().isSuccessful()) {
                        return Mono.error(new IOException("Failed to download file: " + response.response().sdkHttpResponse()));
                    }
                    String filename = getMetadataItem(response.response(),"filename", fileKey);
                    String contentType = response.response().contentType();
                    Long contentLength = response.response().contentLength();
                    File file = new File(filename, contentType, contentLength, response.asByteBuffer(), response.response().metadata());
                    return Mono.just(file);
                });
    }

    @SneakyThrows
    public String storeFile() {
        byte[] data = Files.readAllBytes(Paths.get("src/test/resources/images/albondigas.png"));
        ByteBuffer fileContent = ByteBuffer.wrap(data);
        Map<String, String> fileMetadata = Map.of("entity", "recipe", "entityid", "entityId");

        AtomicReference<String> fileId = new AtomicReference<>();
        fileStorage.storeFile("recipes/images/", IMAGE_PNG_VALUE, data.length, Flux.just(fileContent), fileMetadata)
                .as(StepVerifier::create)
                .consumeNextWith(fileId::set)
                .verifyComplete();

        return fileId.get();
    }

    private String getMetadataItem(GetObjectResponse sdkResponse, String key, String defaultValue) {
        for (Map.Entry<String, String> entry : sdkResponse.metadata()
                .entrySet()) {
            if (entry.getKey()
                    .equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return defaultValue;
    }

    record File(String fileName, String contentType, long contentLength, ByteBuffer content, Map<String, String> metadata) {}
}
