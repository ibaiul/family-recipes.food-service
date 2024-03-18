package eus.ibai.family.recipes.food.rm.domain.file;

import eus.ibai.family.recipes.food.file.StorageFile;
import eus.ibai.family.recipes.food.rm.infrastructure.config.AwsConfig;
import eus.ibai.family.recipes.food.rm.infrastructure.config.S3Properties;
import eus.ibai.family.recipes.food.rm.infrastructure.file.S3FileStorage;
import io.micrometer.core.instrument.DistributionSummary;
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
import reactor.test.StepVerifier;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static eus.ibai.family.recipes.food.test.FileTestUtils.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
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
    void should_download_file() {
        String imageId = storeRecipeImage(s3Client);

        AtomicReference<StorageFile> downloadResponse = new AtomicReference<>();
        fileStorage.retrieveFile("recipes/images/" + imageId)
                .as(StepVerifier::create)
                .consumeNextWith(file -> {
                    byte[] dst = new byte[(int) file.contentLength()];
                    file.content().get(dst);
                    downloadResponse.set(new StorageFile(file.fileName(), file.contentType(), file.contentLength(), ByteBuffer.wrap(dst), file.metadata()));
                })
                .verifyComplete();

        verifyDownloadedRecipeImage(downloadResponse.get().content());
        verifyDownloadSucceededMetricRecorded(1);
    }

    @ParameterizedTest
    @MethodSource
    void should_map_exception_when_downloading_file_fails(CompletableFuture<PutObjectResponse> s3Response, Class<? extends Throwable> expectedException) throws IOException {
        doReturn(s3Response).when(s3Client).getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class));

        fileStorage.retrieveFile("recipes/images/imageId")
                .as(StepVerifier::create)
                .verifyError(expectedException);

        verifyDownloadFailedMetricRecorded(1);
    }

    static Stream<Arguments> should_map_exception_when_downloading_file_fails() {
        return Stream.of(
            Arguments.of(CompletableFuture.failedFuture(NoSuchKeyException.builder().build()), FileNotFoundException.class),
            Arguments.of(CompletableFuture.failedFuture(new Throwable("")), IOException.class),
            Arguments.of(CompletableFuture.completedFuture(ResponseBytes.fromByteArray(GetObjectResponse.builder().sdkHttpResponse(SdkHttpResponse.builder().statusCode(500).build()).build(), new byte[0])), IOException.class)
        );
    }

    private void verifyDownloadSucceededMetricRecorded(int expected) {
        verifyS3MetricRecorded("download", "succeeded", expected);
    }

    private void verifyDownloadFailedMetricRecorded(int expected) {
        verifyS3MetricRecorded("download", "failed", expected);
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
}
