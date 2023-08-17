package eus.ibai.family.recipes.food.test;

import eus.ibai.family.recipes.food.security.AuthenticationRequestDto;
import eus.ibai.family.recipes.food.security.JwtResponseDto;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class TestUtils {

    private TestUtils() {}

    public static final String UUID_PATTERN_STRING = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

    public static final Pattern UUID_PATTERN = Pattern.compile(UUID_PATTERN_STRING);

    public static final Clock FIXED_CLOCK = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));

    public static LocalDateTime fixedTime() {
        return LocalDateTime.now(FIXED_CLOCK);
    }

    public static JwtResponseDto authenticate(WebTestClient webTestClient) {
        return webTestClient.post()
                .uri("/authentication/login")
                .bodyValue(new AuthenticationRequestDto("user1", "pass"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(JwtResponseDto.class)
                .returnResult()
                .getResponseBody();
    }

    public static void stubNewRelicSendMetricResponse() {
        stubFor(post(urlEqualTo("/metric/v1"))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo("application/json; charset=UTF-8"))
                .withRequestBody(matching(".+"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "requestId": "00000000-0000-0000-0000-000000000000"
                                }
                                """)));
    }

    public static void execute(Executable executable) {
        new Thread(() -> {
            try {
                executable.execute();
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to execute in a different thread", e);
            }
        }).start();
    }
}
