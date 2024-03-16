package eus.ibai.family.recipes.food.wm.domain.file;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.util.Map;

public interface FileStorage {

    Mono<String> storeFile(String path, String mediaType, long length, Flux<ByteBuffer> fileContent, Map<String, String> metadata);

    Mono<Void> deleteFile(String path);
}
