package eus.ibai.family.recipes.food.rm.domain.file;

import eus.ibai.family.recipes.food.file.StorageFile;
import reactor.core.publisher.Mono;

public interface FileStorage {

    Mono<StorageFile> retrieveFile(String path);
}
