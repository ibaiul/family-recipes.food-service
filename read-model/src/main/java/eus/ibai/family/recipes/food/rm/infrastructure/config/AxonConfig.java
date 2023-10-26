package eus.ibai.family.recipes.food.rm.infrastructure.config;

import eus.ibai.family.recipes.food.rm.infrastructure.metric.QueryDispatchedInterceptor;
import eus.ibai.family.recipes.food.rm.infrastructure.metric.QueryProcessedInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.TrackedEventMessage;
import org.axonframework.eventhandling.TrackingEventProcessorConfiguration;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.StreamableMessageSource;
import org.axonframework.queryhandling.QueryBus;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration
@RequiredArgsConstructor
public class AxonConfig {

    public static final String PROJECTIONS_EVENT_PROCESSOR = "projections-event-processor";

    public static final String SSE_EVENT_PROCESSOR = "sse-event-processor";

    public static final String RECIPE_TAG_METRICS_EVENT_PROCESSOR = "recipe-tag-metrics-event-processor";

    private final MeterRegistry meterRegistry;

    @Autowired
    public void configureEventProcessing(EventProcessingConfigurer configurer,
                                         @Value("${axon.eventhandling.custom-processors.recipe-tag-metrics-event-processor.event-availability-timeout-seconds}") int recipeTagsTepEat,
                                         @Value("${axon.eventhandling.custom-processors.recipe-tag-metrics-event-processor.token-claim-interval-seconds}") int recipeTagsTepTci) {
        configurer.registerTrackingEventProcessor(RECIPE_TAG_METRICS_EVENT_PROCESSOR, streamableMessageSource(RECIPE_TAG_METRICS_EVENT_PROCESSOR), tepConfiguration(recipeTagsTepEat, recipeTagsTepTci))
                .usingTrackingEventProcessors();
    }

    @Autowired
    public void configureQueryBus(QueryBus queryBus) {
        queryBus.registerDispatchInterceptor(new QueryDispatchedInterceptor(meterRegistry));
        queryBus.registerHandlerInterceptor(new QueryProcessedInterceptor(meterRegistry));
    }

    @NotNull
    private Function<org.axonframework.config.Configuration, StreamableMessageSource<TrackedEventMessage<?>>> streamableMessageSource(String tepName) {
        return config -> {
            EventBus eventBus = config.eventBus();
            if (!(eventBus instanceof EventStore)) {
                throw new AxonConfigurationException("Cannot create Tracking Event Processor with name '" + tepName + "'. " +
                        "The available EventBus does not support tracking processors."
                );
            }
            return (EventStore) eventBus;
        };
    }

    @NotNull
    private Function<org.axonframework.config.Configuration, TrackingEventProcessorConfiguration> tepConfiguration(int eventAvailabilityTimeout, int tokenClaimInterval) {
        return config -> TrackingEventProcessorConfiguration.forSingleThreadedProcessing()
                .andEventAvailabilityTimeout(eventAvailabilityTimeout, SECONDS)
                .andTokenClaimInterval(tokenClaimInterval, SECONDS);
    }
}
