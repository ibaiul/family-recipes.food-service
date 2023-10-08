package eus.ibai.family.recipes.food.wm.infrastructure.axon;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import eus.ibai.family.recipes.food.security.JwtService;
import eus.ibai.family.recipes.food.wm.application.dto.CreateRecipeDto;
import eus.ibai.family.recipes.food.wm.domain.recipe.CreateRecipeCommand;
import eus.ibai.family.recipes.food.wm.domain.recipe.RecipeIngredientAlreadyAddedException;
import eus.ibai.family.recipes.food.wm.infrastructure.exception.DownstreamConnectivityException;
import eus.ibai.family.recipes.food.wm.infrastructure.zookeeper.ZookeeperContainer;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceType;
import org.apache.curator.x.discovery.UriSpec;
import org.axonframework.commandhandling.CommandResultMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.GenericCommandResultMessage;
import org.axonframework.commandhandling.distributed.Member;
import org.axonframework.extensions.springcloud.commandhandling.SpringCloudCommandRouter;
import org.axonframework.extensions.springcloud.commandhandling.SpringHttpReplyMessage;
import org.axonframework.serialization.json.JacksonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.cloud.zookeeper.discovery.ZookeeperInstance;
import org.springframework.cloud.zookeeper.discovery.ZookeeperServiceInstance;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@ActiveProfiles("axon-distributed")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DistributedCommandBusIT {

    private static final ZookeeperContainer<?> zookeeperContainer = new ZookeeperContainer<>("bitnami/zookeeper")
            .withReuse(true);

    static {
        zookeeperContainer.start();
    }

    @RegisterExtension
    private static final WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .configureStaticDsl(true)
            .build();

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JacksonSerializer jacksonSerializer;

    @MockBean
    private DiscoveryClient discoveryClient;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private SpringCloudCommandRouter distributedCommandBusRouter;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        stubReplicaMemberCapabilitiesResponse();
        ZookeeperInstance mockZookeeperInstance = new ZookeeperInstance("serviceName", "serviceName", Map.of("instance_status", "UP"));
        UriSpec uriSpec = new UriSpec();
        uriSpec.add(new UriSpec.Part("scheme", true));
        uriSpec.add(new UriSpec.Part("://", false));
        uriSpec.add(new UriSpec.Part("address", true));
        uriSpec.add(new UriSpec.Part(":", false));
        uriSpec.add(new UriSpec.Part("port", true));
        ServiceInstance<ZookeeperInstance> mockServiceInstance = new ServiceInstance<>("serviceName", "000000000000-0000-0000-0000-00000000",
                "localhost", wiremock.getPort(), null, mockZookeeperInstance, Instant.now().toEpochMilli(), ServiceType.DYNAMIC,
                uriSpec, true);
        ZookeeperServiceInstance mockZookeeperServiceInstance = new ZookeeperServiceInstance("serviceName", mockServiceInstance);
        when(discoveryClient.getServices()).thenReturn(List.of("serviceName"));
        when(discoveryClient.getInstances("serviceName")).thenReturn(List.of(mockZookeeperServiceInstance));
    }

    @Test
    void should_discover_member_capabilities_of_other_replicas() {
        HeartbeatEvent heartbeatEvent = new HeartbeatEvent(this, 1);
        publisher.publishEvent(heartbeatEvent);
        await().atMost(5, SECONDS).untilAsserted(() -> {
            wiremock.verify(exactly(1), getRequestedFor(urlEqualTo("/member-capabilities")));
        });
    }

    @Test
    void should_be_able_to_route_commands_to_other_replicas() {
        HeartbeatEvent heartbeatEvent = new HeartbeatEvent(this, 1);
        publisher.publishEvent(heartbeatEvent);

        await().atMost(3, SECONDS).untilAsserted(() -> {
            CreateRecipeCommand command = new CreateRecipeCommand("recipe" + generateId(), "recipeName" + generateId());
            GenericCommandMessage<CreateRecipeCommand> commandMessage = new GenericCommandMessage<>(command);
            Optional<Member> destination = distributedCommandBusRouter.findDestination(commandMessage);
            assertThat(destination).isPresent();
            assertThat(destination.get().local()).isFalse();
            assertThat(destination.get().getConnectionEndpoint(URI.class)).isPresent();
            assertThat(destination.get().getConnectionEndpoint(URI.class).get()).hasToString(wiremock.baseUrl());
        });
    }

    @Test
    void should_route_commands_to_other_replicas() {
        stubReplicaRouteCommandResponse();
        HeartbeatEvent heartbeatEvent = new HeartbeatEvent(this, 1);
        publisher.publishEvent(heartbeatEvent);

        await().atMost(3, SECONDS).untilAsserted(() -> {
            CreateRecipeDto dto = new CreateRecipeDto("recipeName");
            String userAccessToken = jwtService.create("user1").block().accessToken();
            webTestClient.post()
                    .uri("/recipes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .exchange()
                    .expectStatus().isCreated();
            wiremock.verify(exactly(1), postRequestedFor(urlEqualTo("/spring-command-bus-connector/command")));
        });
    }

    @ParameterizedTest
    @MethodSource
    void should_route_commands_to_other_replicas_and_map_error_responses(int expectedStatus, Exception remoteException) {
        stubReplicaRouteCommandResponseFailure(remoteException);
        HeartbeatEvent heartbeatEvent = new HeartbeatEvent(this, 1);
        publisher.publishEvent(heartbeatEvent);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            CreateRecipeDto dto = new CreateRecipeDto("recipeName");
            String userAccessToken = jwtService.create("user1").block().accessToken();
            webTestClient.post()
                    .uri("/recipes")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(dto))
                    .exchange()
                    .expectStatus().isEqualTo(expectedStatus);
            wiremock.verify(exactly(1), postRequestedFor(urlEqualTo("/spring-command-bus-connector/command")));
        });
    }

    private static Stream<Arguments> should_route_commands_to_other_replicas_and_map_error_responses() {
        return Stream.of(
                Arguments.of(500, new DownstreamConnectivityException("Root message", new RuntimeException("Inner message"))),
                Arguments.of(409, new RecipeIngredientAlreadyAddedException("Root message"))
        );
    }

    @DynamicPropertySource
    public static void setDatasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.zookeeper.connect-string", () -> "localhost:" + zookeeperContainer.getMappedHttpPort());
    }

    public static void stubReplicaMemberCapabilitiesResponse() {
        stubFor(get(urlEqualTo("/member-capabilities"))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "loadFactor": 100,
                                  "serializedCommandFilterType": "org.axonframework.commandhandling.distributed.commandfilter.AcceptAll",
                                  "serializedCommandFilter": "[\\"org.axonframework.commandhandling.distributed.commandfilter.AcceptAll\\",\\"INSTANCE\\"]"
                                }
                                """)));
    }

    public static void stubReplicaRouteCommandResponse() {
        stubFor(post(urlEqualTo("/spring-command-bus-connector/command"))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(matchingJsonPath("$.commandIdentifier", matching("[a-z0-9-]{36}")))
                .withRequestBody(matchingJsonPath("$.commandName", equalTo("eus.ibai.family.recipes.food.wm.domain.recipe.CreateRecipeCommand")))
                .withRequestBody(matchingJsonPath("$.payloadType", equalTo("eus.ibai.family.recipes.food.wm.domain.recipe.CreateRecipeCommand")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "commandIdentifier": "4710b397-f0ab-4998-b642-7a2ba1727c9b",
                                  "serializedMetaData": "eyJ0cmFjZUlkIjoiNDcxMGIzOTctZjBhYi00OTk4LWI2NDItN2EyYmExNzI3YzliIiwiY29ycmVsYXRpb25JZCI6IjQ3MTBiMzk3LWYwYWItNDk5OC1iNjQyLTdhMmJhMTcyN2M5YiJ9",
                                  "payloadType": "java.lang.String",
                                  "payloadRevision": null,
                                  "serializedPayload": "IjMwYzhlMGYwLTcxMWUtNDMzMy04YjU0LWYwNTM1OGZlNmI2ZSI=",
                                  "exceptionType": "empty",
                                  "exceptionRevision": null,
                                  "serializedException": "bnVsbA=="
                                }
                                """)));
    }

    private void stubReplicaRouteCommandResponseFailure(Exception remoteException) {
        CommandResultMessage<Void> commandResultMessage = GenericCommandResultMessage.asCommandResultMessage(remoteException);
        SpringHttpReplyMessage<Void> springHttpReplyMessage = new SpringHttpReplyMessage<>("commandIdentifier", commandResultMessage, jacksonSerializer);

        stubFor(post(urlEqualTo("/spring-command-bus-connector/command"))
                .withHeader(HttpHeaders.AUTHORIZATION, matching("Bearer .+"))
                .withRequestBody(matchingJsonPath("$.commandIdentifier", matching("[a-z0-9-]{36}")))
                .withRequestBody(matchingJsonPath("$.commandName", equalTo("eus.ibai.family.recipes.food.wm.domain.recipe.CreateRecipeCommand")))
                .withRequestBody(matchingJsonPath("$.payloadType", equalTo("eus.ibai.family.recipes.food.wm.domain.recipe.CreateRecipeCommand")))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "commandIdentifier": "%s",
                                  "serializedMetaData": "e30=",
                                  "payloadType": "empty",
                                  "payloadRevision": null,
                                  "serializedPayload": "bnVsbA==",
                                  "exceptionType": "org.axonframework.messaging.RemoteExceptionDescription",
                                  "exceptionRevision": null,
                                  "serializedException": "%s"
                                }
                                """.formatted(springHttpReplyMessage.getCommandIdentifier(), new String(Base64.getEncoder().encode(springHttpReplyMessage.getSerializedException()))))
                ));
    }
}
