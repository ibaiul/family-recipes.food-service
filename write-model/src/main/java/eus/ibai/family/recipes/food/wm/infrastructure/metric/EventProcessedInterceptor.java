package eus.ibai.family.recipes.food.wm.infrastructure.metric;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;

@Slf4j
@RequiredArgsConstructor
public class EventProcessedInterceptor implements MessageHandlerInterceptor<EventMessage<?>> {

    private final MeterRegistry meterRegistry;

    @Override
    public Object handle(@Nonnull UnitOfWork<? extends EventMessage<?>> unitOfWork, @Nonnull InterceptorChain interceptorChain) throws Exception {
        EventMessage<?> eventMessage = unitOfWork.getMessage();
        log.debug("Processing event: {}, Payload: {}", eventMessage.getPayloadType(), eventMessage.getPayload());
        unitOfWork.afterCommit(u -> DistributionSummary.builder("axon.event")
                    .tag("type", eventMessage.getPayloadType().getSimpleName())
                .tag("status", "processed")
                    .register(meterRegistry)
                    .record(1));
        return interceptorChain.proceed();
    }
}
