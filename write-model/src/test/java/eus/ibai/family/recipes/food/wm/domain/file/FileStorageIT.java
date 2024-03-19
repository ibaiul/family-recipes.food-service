package eus.ibai.family.recipes.food.wm.domain.file;

import eus.ibai.family.recipes.food.wm.infrastructure.config.AwsConfig;
import eus.ibai.family.recipes.food.wm.infrastructure.config.S3Properties;
import eus.ibai.family.recipes.food.wm.infrastructure.file.S3FileStorage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static eus.ibai.family.recipes.food.test.FileTestUtils.*;
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
        S3Properties s3Properties = new S3Properties(localstack.getEndpoint().toASCIIString(), localstack.getRegion(), localstack.getAccessKey(), localstack.getSecretKey());
        s3Client = spy(new AwsConfig().s3client(s3Properties));
        fileStorage = new S3FileStorage(s3Client, TEST_BUCKET, meterRegistry);
        createS3Bucket(s3Client);
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

        verifyStoredRecipeImage(s3Client, fileId.get(), IMAGE_PNG_VALUE, data.length, fileMetadata);
        verifyS3MetricRecorded(meterRegistry, "upload", "succeeded", 1);
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

        verifyS3MetricRecorded(meterRegistry, "upload", "failed", 1);
    }

    @Test
    void should_delete_file() {
        String imageId = storeRecipeImage(s3Client);

        fileStorage.deleteFile("recipes/images/" + imageId)
                .as(StepVerifier::create)
                .verifyComplete();

        verifyS3MetricRecorded(meterRegistry, "delete", "succeeded", 1);
    }

    @ParameterizedTest
    @MethodSource
    void should_map_exception_when_deleting_file_fails(CompletableFuture<DeleteObjectResponse> s3Response) {
        doReturn(s3Response).when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        fileStorage.deleteFile("recipes/images/imageId")
                .as(StepVerifier::create)
                .verifyError(IOException.class);

        verifyS3MetricRecorded(meterRegistry, "delete", "failed", 1);
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
}
