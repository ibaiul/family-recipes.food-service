package eus.ibai.family.recipes.food.wm.application.controller;

import eus.ibai.family.recipes.food.security.JwtService;
import eus.ibai.family.recipes.food.wm.domain.recipe.CreateRecipeCommand;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.RecipeNameConstraintEntity;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.RecipeNameConstraintRepository;
import eus.ibai.family.recipes.food.wm.infrastructure.exception.DownstreamConnectivityException;
import eus.ibai.family.recipes.food.wm.infrastructure.zookeeper.ZookeeperContainer;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.commandhandling.CommandResultMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.extensions.springcloud.commandhandling.SpringHttpDispatchMessage;
import org.axonframework.extensions.springcloud.commandhandling.SpringHttpReplyMessage;
import org.axonframework.messaging.IllegalPayloadAccessException;
import org.axonframework.messaging.RemoteHandlingException;
import org.axonframework.serialization.json.JacksonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ActiveProfiles("axon-distributed")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DistributedCommandBusControllerIT {

    @Container
    private static final ZookeeperContainer<?> zookeeperContainer = new ZookeeperContainer<>("bitnami/zookeeper")
            .withReuse(true);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JacksonSerializer jacksonSerializer;

    @MockBean
    private RecipeNameConstraintRepository recipeNameConstraintRepository;

    @Autowired
    private WebTestClient webTestClient;

    private String bearerToken;

    @BeforeEach
    void beforeEach() {
        bearerToken = jwtService.create("serviceName").block().accessToken();
    }

    @Test
    void should_accept_all_commands() {
        webTestClient.get()
                .uri("/member-capabilities")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.loadFactor").isEqualTo(100)
                .jsonPath("$.serializedCommandFilterType").isEqualTo("org.axonframework.commandhandling.distributed.commandfilter.AcceptAll")
                .jsonPath("$.serializedCommandFilter").isEqualTo("[\"org.axonframework.commandhandling.distributed.commandfilter.AcceptAll\",\"INSTANCE\"]");
    }

    @Test
    void should_handle_routed_commands_from_other_replicas() {
        String aggregateId = UUID.randomUUID().toString();
        CreateRecipeCommand command = new CreateRecipeCommand(aggregateId, "recipeName" + aggregateId);
        GenericCommandMessage<CreateRecipeCommand> commandMessage = new GenericCommandMessage<>(command);
        SpringHttpDispatchMessage<CreateRecipeCommand> routedCommandEnvelope = new SpringHttpDispatchMessage<>(commandMessage, jacksonSerializer, true);

        webTestClient.post()
                .uri("/spring-command-bus-connector/command")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .bodyValue(routedCommandEnvelope)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<SpringHttpReplyMessage<?>>() {})
                .value(response -> {
                    assertThat(response.getCommandIdentifier()).isNotNull();
                    assertThat(response.getExceptionType()).isEqualTo("empty");
                    assertThat(response.getPayloadType()).isEqualTo("java.lang.String");
                    CommandResultMessage<?> commandResultMessage = response.getCommandResultMessage(jacksonSerializer);
                    assertThat(commandResultMessage.getIdentifier()).isNotNull();
                    assertThat(commandResultMessage.getPayload()).isEqualTo(aggregateId);
                    assertThat(commandResultMessage.exceptionDetails()).isEmpty();
                    assertThat(commandResultMessage.isExceptional()).isFalse();
                    assertThat(commandResultMessage.getMetaData()).containsKey("traceId");
                    assertThat(commandResultMessage.getMetaData()).containsKey("correlationId");
                });
    }

    @Test
    void should_propagate_errors_when_handling_routed_commands_fails() {
        CreateRecipeCommand command = new CreateRecipeCommand(UUID.randomUUID().toString(), "collidingRecipeName");
        DownstreamConnectivityException remoteException = new DownstreamConnectivityException("Root message", new RuntimeException("Inner message"));
        String expectedRemoteExceptionMessage = format("An exception was thrown by the remote message handling component: %s: %s\nCaused by %s", remoteException.getClass().getCanonicalName(), remoteException.getMessage(), remoteException.getCause());
        when(recipeNameConstraintRepository.save(new RecipeNameConstraintEntity(command.aggregateId(), command.recipeName()))).thenThrow(remoteException);
        GenericCommandMessage<CreateRecipeCommand> commandMessage = new GenericCommandMessage<>(command);
        SpringHttpDispatchMessage<CreateRecipeCommand> routedCommandEnvelope = new SpringHttpDispatchMessage<>(commandMessage, jacksonSerializer, true);

        webTestClient.post()
                .uri("/spring-command-bus-connector/command")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .bodyValue(routedCommandEnvelope)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<SpringHttpReplyMessage<?>>() {})
                .value(response -> {
                    assertThat(response.getCommandIdentifier()).isNotNull();
                    assertThat(response.getExceptionType()).isEqualTo("org.axonframework.messaging.RemoteExceptionDescription");
                    assertThat(response.getPayloadType()).isEqualTo("empty");
                    CommandResultMessage<?> commandResultMessage = response.getCommandResultMessage(jacksonSerializer);
                    assertThat(commandResultMessage.getIdentifier()).isNotNull();
                    assertThrows(IllegalPayloadAccessException.class, commandResultMessage::getPayload);
                    assertThat(commandResultMessage.isExceptional()).isTrue();
                    assertThat(commandResultMessage.exceptionResult()).isExactlyInstanceOf(CommandExecutionException.class);
                    assertThat(commandResultMessage.exceptionResult().getCause()).isExactlyInstanceOf(RemoteHandlingException.class);
                    assertThat(commandResultMessage.exceptionResult().getCause().getMessage()).isEqualTo(expectedRemoteExceptionMessage);
                    assertThat(commandResultMessage.exceptionDetails()).isEmpty();
                    assertThat(commandResultMessage.getMetaData()).isEmpty();
                });
    }

    @DynamicPropertySource
    public static void setDynamicProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.zookeeper.connect-string", () -> "localhost:" + zookeeperContainer.getHttpPort());
    }
}
