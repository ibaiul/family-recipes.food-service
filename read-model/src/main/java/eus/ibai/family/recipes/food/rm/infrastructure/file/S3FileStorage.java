package eus.ibai.family.recipes.food.rm.infrastructure.file;

import eus.ibai.family.recipes.food.file.StorageFile;
import eus.ibai.family.recipes.food.rm.domain.file.FileStorage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

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
    public Mono<StorageFile> retrieveFile(String fileKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
                .build();

        return Mono.fromFuture(s3Client.getObject(request, AsyncResponseTransformer.toBytes()))
                .flatMap(response -> {
                    if (response.response().sdkHttpResponse() == null || !response.response().sdkHttpResponse().isSuccessful()) {
                        return Mono.error(new IOException("Failed to download file: " + response.response().sdkHttpResponse()));
                    }
                    String contentType = response.response().contentType();
                    Long contentLength = response.response().contentLength();
                    StorageFile storageFile = new StorageFile(fileKey, contentType, contentLength, response.asByteBuffer(), response.response().metadata());
                    return Mono.just(storageFile);
                })
                .onErrorMap(t -> !(t instanceof NoSuchKeyException), IOException::new)
                .onErrorMap(NoSuchKeyException.class, t -> new FileNotFoundException("File not found: " + fileKey))
                .doOnNext(fileId -> meterRegistry.summary("s3.download", List.of(Tag.of("status", "succeeded"))).record(1))
                .doOnError(t -> meterRegistry.summary("s3.download", List.of(Tag.of("status", "failed"))).record(1));
    }
}
