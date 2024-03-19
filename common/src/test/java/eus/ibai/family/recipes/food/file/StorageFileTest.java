package eus.ibai.family.recipes.food.file;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class StorageFileTest {

    @ParameterizedTest
    @MethodSource
    void should_get_file_name(String storagePath, String expectedFileName) {
        StorageFile storageFile = new StorageFile(storagePath, "", 0, null, null);

        assertThat(storageFile.fileName()).isEqualTo(expectedFileName);
    }

    private static Stream<Arguments> should_get_file_name() {
        return Stream.of(
            Arguments.of("recipes/images/imageId", "imageId"),
            Arguments.of("imageId", "imageId")
        );
    }
}