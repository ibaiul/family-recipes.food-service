package eus.ibai.family.recipes.food.file;

import java.nio.ByteBuffer;
import java.util.Map;

public record StorageFile(String storagePath, String contentType, long contentLength, ByteBuffer content, Map<String, String> metadata) {

    public String fileName() {
        int pathSeparatorIndex = storagePath.lastIndexOf('/');
        int fileNameIndex = pathSeparatorIndex != -1 ? pathSeparatorIndex + 1 : 0;
        return storagePath.substring(fileNameIndex);
    }
}
