package eus.ibai.family.recipes.food.rm.infrastructure.metric;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.MessageDispatchInterceptor;
import org.axonframework.queryhandling.QueryMessage;

import java.util.List;
import java.util.function.BiFunction;

@Slf4j
@RequiredArgsConstructor
public class QueryDispatchedInterceptor implements MessageDispatchInterceptor<QueryMessage<?, ?>> {

    private final MeterRegistry meterRegistry;

    @Nonnull
    @Override
    public BiFunction<Integer, QueryMessage<?,?>, QueryMessage<?, ?>> handle(@Nonnull List<? extends QueryMessage<?, ?>> messages) {
        return (index, commandMessage) -> {
            log.debug("Query received: {}, Payload: {}", commandMessage.getPayloadType(), commandMessage.getPayload());
            DistributionSummary.builder("axon.query")
                    .tag("type", commandMessage.getPayloadType().getSimpleName())
                    .tag("status", "dispatched")
                    .register(meterRegistry)
                    .record(1);
            return commandMessage;
        };
    }
}
