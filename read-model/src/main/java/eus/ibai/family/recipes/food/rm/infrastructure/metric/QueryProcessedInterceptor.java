package eus.ibai.family.recipes.food.rm.infrastructure.metric;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.queryhandling.QueryMessage;

@Slf4j
@RequiredArgsConstructor
public class QueryProcessedInterceptor implements MessageHandlerInterceptor<QueryMessage<?, ?>> {

    private final MeterRegistry meterRegistry;

    @Override
    public Object handle(@Nonnull UnitOfWork<? extends QueryMessage<?, ?>> unitOfWork, @Nonnull InterceptorChain interceptorChain) throws Exception {
        QueryMessage<?, ?> queryMessage = unitOfWork.getMessage();
        log.debug("Query received: {}, Payload: {}", queryMessage.getPayloadType(), queryMessage.getPayload());
        unitOfWork.afterCommit(u -> DistributionSummary.builder("axon.query")
                .tag("type", queryMessage.getPayloadType().getSimpleName())
                .tag("status", "processed")
                .register(meterRegistry)
                .record(1));
        return interceptorChain.proceed();
    }
}
