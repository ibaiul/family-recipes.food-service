package eus.ibai.family.recipes.food.wm.infrastructure.metric;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.BiFunction;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventDispatchedInterceptor implements MessageDispatchInterceptor<EventMessage<?>> {

    private final MeterRegistry meterRegistry;

    @Nonnull
    @Override
    public BiFunction<Integer, EventMessage<?>, EventMessage<?>> handle(@Nonnull List<? extends EventMessage<?>> messages) {
        return (index, eventMessage) -> {
            log.debug("Dispatching event: {}, Payload: {}", eventMessage.getPayloadType(), eventMessage.getPayload());
            DistributionSummary.builder("axon.event")
                    .tag("type", eventMessage.getPayloadType().getSimpleName())
                    .tag("status", "dispatched")
                    .register(meterRegistry)
                    .record(1);
            return eventMessage;
        };
    }
}
