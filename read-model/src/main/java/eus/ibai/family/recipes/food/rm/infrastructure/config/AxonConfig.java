package eus.ibai.family.recipes.food.rm.infrastructure.config;

import eus.ibai.family.recipes.food.rm.infrastructure.metric.QueryDispatchedInterceptor;
import eus.ibai.family.recipes.food.rm.infrastructure.metric.QueryProcessedInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.queryhandling.QueryBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AxonConfig {

    public static final String PROJECTIONS_EVENT_PROCESSOR = "projections-event-processor";

    public static final String SSE_EVENT_PROCESSOR = "sse-event-processor";

    public static final String RECIPE_TAG_METRICS_EVENT_PROCESSOR = "recipe-tag-metrics-event-processor";

    private final MeterRegistry meterRegistry;

    @Autowired
    public void configureEventProcessing(EventProcessingConfigurer configurer) {
        configurer.usingTrackingEventProcessors();
    }

    @Autowired
    public void configureQueryBus(QueryBus queryBus) {
        queryBus.registerDispatchInterceptor(new QueryDispatchedInterceptor(meterRegistry));
        queryBus.registerHandlerInterceptor(new QueryProcessedInterceptor(meterRegistry));
    }
}
