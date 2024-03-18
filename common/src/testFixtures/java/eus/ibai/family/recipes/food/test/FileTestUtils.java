package eus.ibai.family.recipes.food.test;

import eus.ibai.family.recipes.food.file.StorageFile;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class FileTestUtils {

    public static final String TEST_BUCKET = "test-bucket";

    @SneakyThrows
    public static void createS3Bucket(S3AsyncClient s3Client) {
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket(TEST_BUCKET)
                .acl(BucketCannedACL.PRIVATE)
                .build();
        CreateBucketResponse createBucketResponse = s3Client.createBucket(createBucketRequest).get();
        assertThat(createBucketResponse.sdkHttpResponse().isSuccessful()).isTrue();
    }

    public static void verifyStoredRecipeImage(S3AsyncClient s3Client, String imageId, String contentType, long contentLength, Map<String, String> imageMetadata) {
        StorageFile storageFile = retrieveFile(s3Client, imageId);
        assertThat(storageFile.fileName()).isEqualTo(imageId);
        assertThat(storageFile.contentType()).isEqualTo(contentType);
        assertThat(storageFile.contentLength()).isEqualTo(contentLength);
        assertThat(storageFile.metadata()).containsExactlyInAnyOrderEntriesOf(imageMetadata);
    }

    @SneakyThrows
    public static void verifyDownloadedRecipeImage(ByteBuffer downloadResponse) {
        java.io.File recipeImage = Files.createTempFile("foo", "bar").toFile();
        recipeImage.deleteOnExit();
        try(FileOutputStream fos = new FileOutputStream(recipeImage); BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            bos.write(downloadResponse.array());
        }

        assertThat(recipeImage).hasSize(24018);
        assertThat(recipeImage).hasDigest("sha256", "4b256f5fd63ab3b6fc4395b537bf553f357a76b4506276613de977e7f1abd340");
    }

    @SneakyThrows
    private static StorageFile retrieveFile(S3AsyncClient s3Client, String imageId) {
        String fileKey = "recipes/images/" + imageId;
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(TEST_BUCKET)
                .key(fileKey)
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObject(request, AsyncResponseTransformer.toBytes()).get();
        GetObjectResponse getObjectResponse = response.response();

        assertThat(getObjectResponse.sdkHttpResponse().isSuccessful()).isTrue();

        String contentType = getObjectResponse.contentType();
        Long contentLength = getObjectResponse.contentLength();
        return new StorageFile(fileKey, contentType, contentLength, response.asByteBuffer(), getObjectResponse.metadata());
    }

    @SneakyThrows
    public static String storeRecipeImage(S3AsyncClient s3Client) {
        byte[] data = Files.readAllBytes(Paths.get("../common/src/testFixtures/resources/images/albondigas.png"));
        Map<String, String> fileMetadata = Map.of("entity", "recipe", "entityid", "entityId");

        String fileKey = UUID.randomUUID().toString();
        PutObjectRequest request = PutObjectRequest.builder()
                .acl(ObjectCannedACL.PRIVATE)
                .bucket(TEST_BUCKET)
                .contentLength((long) data.length)
                .key("recipes/images/" + fileKey)
                .contentType("image/png")
                .metadata(fileMetadata)
                .build();
        PutObjectResponse putObjectResponse = s3Client.putObject(request, AsyncRequestBody.fromBytes(data)).get();

        assertThat(putObjectResponse.sdkHttpResponse().isSuccessful()).isTrue();

        return fileKey;
    }

    public static void verifyS3MetricRecorded(MeterRegistry meterRegistry, String action, String status, int expected) {
        await().atMost(1, SECONDS).untilAsserted(() -> {
            DistributionSummary metric = meterRegistry.find("s3." + action)
                    .tag("status", status)
                    .summary();
            assertThat(metric).isNotNull();
            assertThat(metric.count()).isEqualTo(expected);
        });
    }
}
