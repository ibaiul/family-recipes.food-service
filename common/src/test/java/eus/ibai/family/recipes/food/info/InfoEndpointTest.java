package eus.ibai.family.recipes.food.info;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.sql.SQLException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InfoEndpointTest {

    @Autowired
    private ApplicationContext applicationContext;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();
    }

    @Test
    void should_return_application_info() throws SQLException {
        webTestClient.get().uri("/actuator/info")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.app.name").isEqualTo("Family Recipes")
                .jsonPath("$.app.description").exists()
                .jsonPath("$.app.service").isEqualTo("Food Service (Common)")
                .jsonPath("$.app.version").isEqualTo("1.0.0")
                .jsonPath("$.git.branch").exists()
                .jsonPath("$.git.commit.id").exists()
                .jsonPath("$.build.artifact").exists()
                .jsonPath("$.build.group").exists()
                .jsonPath("$.build.name").exists()
                .jsonPath("$.build.version").exists();
    }
}
