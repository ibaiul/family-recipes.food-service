package eus.ibai.family.recipes.food.security;

import eus.ibai.family.recipes.food.test.TestController;
import eus.ibai.family.recipes.food.test.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.stream.Stream;

import static eus.ibai.family.recipes.food.test.TestUtils.authenticate;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.WWW_AUTHENTICATE;

@WebFluxTest(controllers = {AuthController.class, TestController.class})
@Import({GlobalSecurityConfig.class, TestSecurityConfig.class, JwtService.class, JwtProperties.class, UserProperties.class})
class AuthControllerIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.access-token.expiration-time}")
    private long accessTokenExpirationTime;

    @Test
    void should_get_jwt_tokens_when_login() {
        webTestClient.post()
                .uri("/authentication/login")
                .bodyValue(new AuthenticationRequestDto("user1", "pass"))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectHeader().cacheControl(CacheControl.noStore())
                .expectHeader().valueEquals("Pragma", "no-cache")
                .expectBody()
                .jsonPath("$.access_token").isNotEmpty()
                .jsonPath("$.token_type").isEqualTo("Bearer")
                .jsonPath("$.scope").isEqualTo("")
                .jsonPath("$.expires_in").isEqualTo(1)
                .jsonPath("$.refresh_token").isNotEmpty();
    }

    @Test
    void should_not_get_jwt_tokens_when_login_with_wrong_credentials() {
        webTestClient.post()
                .uri("/authentication/login")
                .bodyValue(new AuthenticationRequestDto("user1", "pa$$"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueMatches(WWW_AUTHENTICATE, "Bearer");
    }

    @Test
    void should_refresh_access_token_when_refresh_token_is_valid() {
        JwtResponseDto credentials = authenticate(webTestClient);
        String accessToken = credentials.accessToken();
        String refreshToken = credentials.refreshToken();

        webTestClient.post()
                .uri("/authentication/refresh")
                .bodyValue(new AuthenticationRefreshRequestDto(refreshToken))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectHeader().cacheControl(CacheControl.noStore())
                .expectHeader().valueEquals("Pragma", "no-cache")
                .expectBody()
                .jsonPath("$.access_token").value(not(accessToken))
                .jsonPath("$.token_type").isEqualTo("Bearer")
                .jsonPath("$.scope").isEqualTo("")
                .jsonPath("$.expires_in").isEqualTo(1)
                .jsonPath("$.refresh_token").value(not(refreshToken));
    }

    @Test
    void should_not_refresh_credentials_when_refresh_token_is_malformed() {
        webTestClient.post()
                .uri("/authentication/refresh")
                .bodyValue(new AuthenticationRefreshRequestDto("malformedJwtToken"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueMatches(WWW_AUTHENTICATE, "Bearer");
    }

    @Test
    void should_deny_access_when_access_token_has_expired() {
        String accessToken = authenticate(webTestClient).accessToken();

        await().pollInterval(100, MILLISECONDS).atMost(accessTokenExpirationTime * 2, SECONDS).untilAsserted(() ->
                webTestClient.get()
                        .uri("/test/protected")
                        .header(AUTHORIZATION, "Bearer " + accessToken)
                        .exchange()
                        .expectStatus().isUnauthorized()
                        .expectHeader().valueMatches(WWW_AUTHENTICATE, "Bearer"));
    }

    @ParameterizedTest
    @MethodSource
    void should_fail_to_login_when_credentials_are_malformed(AuthenticationRequestDto malformedDto) {
        webTestClient.post()
                .uri("/authentication/login")
                .bodyValue(malformedDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    private static Stream<AuthenticationRequestDto> should_fail_to_login_when_credentials_are_malformed() {
        return Stream.of(
                new AuthenticationRequestDto(null, "pass"),
                new AuthenticationRequestDto("", "pass"),
                new AuthenticationRequestDto("  ", "pass"),
                new AuthenticationRequestDto("user", null),
                new AuthenticationRequestDto("user", ""),
                new AuthenticationRequestDto("user", "  ")
        );
    }

    @ParameterizedTest
    @MethodSource
    void should_fail_to_refresh_credentials_when_refresh_token_is_malformed(AuthenticationRefreshRequestDto malformedDto) {
        webTestClient.post()
                .uri("/authentication/refresh")
                .bodyValue(malformedDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    private static Stream<AuthenticationRefreshRequestDto> should_fail_to_refresh_credentials_when_refresh_token_is_malformed() {
        return Stream.of(
                new AuthenticationRefreshRequestDto(null),
                new AuthenticationRefreshRequestDto(""),
                new AuthenticationRefreshRequestDto("  ")
        );
    }
}
