package eus.ibai.family.recipes.food.docs;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiTest {

    @Autowired
    private ApplicationContext applicationContext;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();
    }

    @Test
    void should_return_open_api_spec() {
        webTestClient.get().uri("/v3/api-docs")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.openapi").isEqualTo("3.0.1")
                .jsonPath("$.info.title").isEqualTo("Food Service API");
    }

    @Test
    void should_redirect_to_swagger_ui() {
        webTestClient.get().uri("/swagger-ui.html")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().location("/swagger-ui/index.html");
    }

    @Test
    void should_render_swagger_ui() {
        webTestClient.mutate()
                .filter(new FollowRedirects())
                .build()
                .get().uri("/swagger-ui/index.html")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(equalTo("""
                        <!-- HTML for static distribution bundle build -->
                        <!DOCTYPE html>
                        <html lang="en">
                          <head>
                            <meta charset="UTF-8">
                            <title>Swagger UI</title>
                            <link rel="stylesheet" type="text/css" href="./swagger-ui.css" />
                            <link rel="stylesheet" type="text/css" href="index.css" />
                            <link rel="icon" type="image/png" href="./favicon-32x32.png" sizes="32x32" />
                            <link rel="icon" type="image/png" href="./favicon-16x16.png" sizes="16x16" />
                          </head>
                                                
                          <body>
                            <div id="swagger-ui"></div>
                            <script src="./swagger-ui-bundle.js" charset="UTF-8"> </script>
                            <script src="./swagger-ui-standalone-preset.js" charset="UTF-8"> </script>
                            <script src="./swagger-initializer.js" charset="UTF-8"> </script>
                          </body>
                        </html>
                        """));
    }

    private static class FollowRedirects implements ExchangeFilterFunction {

        @NotNull
        @Override
        public Mono<ClientResponse> filter(@NotNull ClientRequest request, ExchangeFunction next) {
            return next.exchange(request).flatMap((response) -> redirectIfNecessary(request, next, response));
        }

        private Mono<ClientResponse> redirectIfNecessary(ClientRequest request, ExchangeFunction next, ClientResponse response) {
            URI location = response.headers().asHttpHeaders().getLocation();
            String host = request.url().getHost();
            String scheme = request.url().getScheme();
            if (location != null) {
                String redirectUrl = location.toASCIIString();
                if (location.getHost() == null) {
                    redirectUrl = scheme + "://" + host + location.toASCIIString();
                }
                ClientRequest redirect = ClientRequest.create(HttpMethod.GET, URI.create(redirectUrl))
                        .headers((headers) -> headers.addAll(request.headers()))
                        .cookies((cookies) -> cookies.addAll(request.cookies()))
                        .attributes((attributes) -> attributes.putAll(request.attributes()))
                        .build();
                return next.exchange(redirect).flatMap((r) -> redirectIfNecessary(request, next, r));
            }
            return Mono.just(response);
        }
    }
}
