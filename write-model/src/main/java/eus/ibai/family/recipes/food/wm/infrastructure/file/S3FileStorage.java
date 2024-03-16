package eus.ibai.family.recipes.food.wm.infrastructure.file;

import eus.ibai.family.recipes.food.wm.domain.file.FileStorage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class S3FileStorage implements FileStorage {

    private final S3AsyncClient s3Client;

    private final String bucket;

    private final MeterRegistry meterRegistry;

    public S3FileStorage(S3AsyncClient s3Client, @Value("${s3.bucket}") String bucket, MeterRegistry meterRegistry) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<String> storeFile(String path, String mediaType, long length, Flux<ByteBuffer> fileContent, Map<String, String> metadata) {
        String fileKey = UUID.randomUUID().toString();
        PutObjectRequest request = PutObjectRequest.builder()
                .acl(ObjectCannedACL.PRIVATE)
                .bucket(bucket)
                .contentLength(length)
                .key(path + fileKey)
                .contentType(mediaType)
                .metadata(metadata)
                .build();
        log.info("SERVUS Uploading file {} to bucket {}", fileKey, bucket);
        return Mono.fromFuture(s3Client.putObject(request, AsyncRequestBody.fromPublisher(fileContent)))
                .flatMap(response -> {
                    if (response.sdkHttpResponse() == null || !response.sdkHttpResponse().isSuccessful()) {
                        return Mono.error(new IOException("Failed to upload file: " + response.sdkHttpResponse()));
                    }
                    return Mono.just(fileKey);
                })
                .doOnNext(fileId -> recordUploadOutcome("succeeded"))
                .doOnError(t -> recordUploadOutcome("failed"))
                .onErrorMap(t -> new IOException("Failed to upload file to S3", t));
    }

    @Override
    public Mono<Void> deleteFile(String path) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build();
        return Mono.fromFuture(s3Client.deleteObject(request))
                .flatMap(response -> {
                    if (response.sdkHttpResponse() == null || !response.sdkHttpResponse().isSuccessful()) {
                        return Mono.error(new IOException("Failed to upload file: " + response.sdkHttpResponse()));
                    }
                    return Mono.empty();
                })
                .doOnSuccess(v -> recordDeleteOutcome("succeeded"))
                .doOnError(t -> recordDeleteOutcome("failed"))
                .onErrorMap(t -> new IOException("Failed to delete file from S3: " + path, t))
                .then();
    }

    private void recordUploadOutcome(String status) {
        meterRegistry.summary("s3.upload", List.of(Tag.of("status", status))).record(1);
    }

    private void recordDeleteOutcome(String status) {
        meterRegistry.summary("s3.delete", List.of(Tag.of("status", status))).record(1);
    }
}
