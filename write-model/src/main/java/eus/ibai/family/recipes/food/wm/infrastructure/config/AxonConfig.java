package eus.ibai.family.recipes.food.wm.infrastructure.config;

import eus.ibai.family.recipes.food.auth.service.ServiceTokenProvider;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.IngredientNameConstraintRepository;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.PropertyNameConstraintRepository;
import eus.ibai.family.recipes.food.wm.infrastructure.constraint.RecipeNameConstraintRepository;
import eus.ibai.family.recipes.food.wm.infrastructure.metric.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.RoutingKey;
import org.axonframework.commandhandling.distributed.AnnotationRoutingStrategy;
import org.axonframework.commandhandling.distributed.CommandBusConnector;
import org.axonframework.commandhandling.distributed.CommandRouter;
import org.axonframework.commandhandling.distributed.DistributedCommandBus;
import org.axonframework.commandhandling.distributed.commandfilter.AcceptAll;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.PropagatingErrorHandler;
import org.axonframework.extensions.springcloud.commandhandling.SpringCloudCommandRouter;
import org.axonframework.extensions.springcloud.commandhandling.mode.AcceptAllCommandsDiscoveryMode;
import org.axonframework.extensions.springcloud.commandhandling.mode.RestCapabilityDiscoveryMode;
import org.axonframework.serialization.json.JacksonSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AxonConfig {

    public static final String CONSTRAINT_EVENT_PROCESSOR = "constraint-event-processor";

    private final MeterRegistry meterRegistry;

    private final RecipeNameConstraintRepository recipeRepository;

    private final IngredientNameConstraintRepository ingredientRepository;

    private final PropertyNameConstraintRepository propertyRepository;

    @Autowired
    public void configureEventProcessing(EventProcessingConfigurer configurer) {
        FoodStatsRecorder foodStatsRecorder = new FoodStatsRecorder(meterRegistry, recipeRepository, ingredientRepository, propertyRepository);
        foodStatsRecorder.recordInitialStats();
        configurer
                .usingSubscribingEventProcessors()
                .registerListenerInvocationErrorHandler(CONSTRAINT_EVENT_PROCESSOR, conf -> PropagatingErrorHandler.instance())
                .registerHandlerInterceptor(CONSTRAINT_EVENT_PROCESSOR, configuration -> foodStatsRecorder)
                .registerHandlerInterceptor(CONSTRAINT_EVENT_PROCESSOR, configuration -> new EventProcessedInterceptor(meterRegistry));
    }

    @Autowired
    public void configureEventBus(EventBus eventBus) {
        eventBus.registerDispatchInterceptor(new EventDispatchedInterceptor(meterRegistry));
    }

    @Autowired
    public void configureCommandBus(CommandBus commandBus) {
        commandBus.registerDispatchInterceptor(new CommandDispatchedInterceptor(meterRegistry));
        commandBus.registerHandlerInterceptor(new CommandProcessedInterceptor(meterRegistry));
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnProperty(name = "axon.distributed.enabled", havingValue = "true")
    public CommandRouter springCloudCommandRouter(DiscoveryClient discoveryClient,
                                                  Registration registration,
                                                  RestCapabilityDiscoveryMode restCapabilityDiscoveryMode,
                                                  JacksonSerializer jacksonSerializer) {
        AnnotationRoutingStrategy routingStrategy = AnnotationRoutingStrategy.builder()
                .annotationType(RoutingKey.class)
                .build();
        AcceptAllCommandsDiscoveryMode capabilityDiscoveryMode = AcceptAllCommandsDiscoveryMode.builder()
                .delegate(restCapabilityDiscoveryMode)
                .build();
        return SpringCloudCommandRouter.builder()
                .discoveryClient(discoveryClient)
                .localServiceInstance(registration)
                .routingStrategy(routingStrategy)
                .capabilityDiscoveryMode(capabilityDiscoveryMode)
                .serializer(jacksonSerializer)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "axon.distributed.enabled", havingValue = "true")
    public RestCapabilityDiscoveryMode restCapabilityDiscoveryMode(RestTemplate restTemplate,
                                                                   @Value("${axon.distributed.spring-cloud.rest-mode-url}") String messageRoutingInformationEndpoint,
                                                                   JacksonSerializer jacksonSerializer,
                                                                   Registration registration) {
        RestCapabilityDiscoveryMode restCapabilityDiscoveryMode = RestCapabilityDiscoveryMode.builder()
                .restTemplate(restTemplate)
                .messageCapabilitiesEndpoint(messageRoutingInformationEndpoint)
                .serializer(jacksonSerializer)
                .build();
        // Register local capabilities ASAP to avoid race condition with Spring Cloud instance registering which causes NPE in Axon
        restCapabilityDiscoveryMode.updateLocalCapabilities(registration, 100, AcceptAll.INSTANCE);
        return restCapabilityDiscoveryMode;
    }

    @Bean("distributedCommandBus")
    @Primary
    @ConditionalOnProperty(name = "axon.distributed.enabled", havingValue = "true")
    public DistributedCommandBus distributedCommandBus(CommandRouter commandRouter, CommandBusConnector commandBusConnector) {
        return DistributedCommandBus.builder()
                .commandRouter(commandRouter)
                .connector(commandBusConnector)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "axon.distributed.enabled", havingValue = "true")
    public RestTemplate restTemplateWithTokenSupplier(ServiceTokenProvider serviceTokenProvider) {
        return new RestTemplateBuilder()
                .requestCustomizers(clientHttpRequest -> {
                    log.trace("Injecting service token");
                    String serviceToken = serviceTokenProvider.getServiceToken()
                            .orElseThrow(() -> new IllegalStateException("Could not retrieve a service token."));
                    clientHttpRequest.getHeaders().setBearerAuth(serviceToken);
                })
                .build();
    }
}
