package eus.ibai.family.recipes.food.security;

import eus.ibai.family.recipes.food.test.TestController;
import eus.ibai.family.recipes.food.test.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import static eus.ibai.family.recipes.food.test.TestUtils.authenticate;
import static org.springframework.http.HttpHeaders.WWW_AUTHENTICATE;

@WebFluxTest(controllers = {TestController.class, AuthController.class})
@Import({GlobalSecurityConfig.class, TestSecurityConfig.class, JwtService.class, JwtProperties.class, UserProperties.class})
class RbacControllerIT {
    
    @Autowired
    private WebTestClient webTestClient;

    private String bearerToken;

    @BeforeEach
    void beforeEach() {
        bearerToken = authenticate(webTestClient).accessToken();
    }

    @Test
    void should_allow_access_when_requesting_open_endpoint_without_credentials() {
        webTestClient.get()
                .uri("/test/open")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void should_allow_access_when_requesting_open_endpoint_and_authenticated() {
        webTestClient.get()
                .uri("/test/open")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void should_deny_access_when_requesting_protected_endpoint_without_credentials() {
        webTestClient.get()
                .uri("/test/protected")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueMatches(WWW_AUTHENTICATE, "Bearer");
    }

    @Test
    void should_deny_access_when_requesting_non_existent_endpoint_without_credentials() {
        webTestClient.get()
                .uri("/non-existent")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueMatches(WWW_AUTHENTICATE, "Bearer");
    }

    @Test
    void should_allow_access_when_requesting_protected_endpoint_with_correct_role() {
        webTestClient.get()
                .uri("/test/protected")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void should_deny_access_when_requesting_protected_endpoint_with_incorrect_role() {
        webTestClient.get()
                .uri("/test/admin")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void should_deny_access_when_requesting_not_security_configured_endpoint_without_credentials() {
        webTestClient.get()
                .uri("/test")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueMatches(WWW_AUTHENTICATE, "Bearer");
    }

    @Test
    void should_deny_access_when_requesting_not_security_configured_endpoint_and_authenticated() {
        webTestClient.get()
                .uri("/test")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueMatches(WWW_AUTHENTICATE, "Bearer");
    }
}
