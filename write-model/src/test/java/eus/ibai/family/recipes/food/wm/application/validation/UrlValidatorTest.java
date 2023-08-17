package eus.ibai.family.recipes.food.wm.application.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UrlValidatorTest {

    private final UrlValidator urlValidator = new UrlValidator();

    @ParameterizedTest
    @MethodSource
    void should_validate_valid_urls(String url) {
        assertThat(urlValidator.isValid(Set.of(url), null)).isTrue();
    }

    private static Stream<String> should_validate_valid_urls() {
        return Stream.of(
                "http://a.aa",
                "https://b.bb",
                "https://c.cc/c1/c2/",
                "https://d.d?d=d,d",
                "https://e.ee?p1=e&p2=e",
                "https://f.ff?p1=%3D%2F&p2=e"
        );
    }

    @ParameterizedTest
    @MethodSource
    void should_validate_invalid_urls(String url) {
        assertThat(urlValidator.isValid(Set.of(url), null)).isFalse();
    }

    private static Stream<String> should_validate_invalid_urls() {
        return Stream.of("ftp://a.aa", "https://", "a.aa");
    }
}
