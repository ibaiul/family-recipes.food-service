package eus.ibai.family.recipes.food.wm.infrastructure.metric;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;

@Slf4j
@RequiredArgsConstructor
public class CommandProcessedInterceptor implements MessageHandlerInterceptor<CommandMessage<?>> {

    private final MeterRegistry meterRegistry;

    @Override
    public Object handle(@Nonnull UnitOfWork<? extends CommandMessage<?>> unitOfWork, @Nonnull InterceptorChain interceptorChain) throws Exception {
        CommandMessage<?> commandMessage = unitOfWork.getMessage();
        log.debug("Processing command: {}, Payload: {}", commandMessage.getPayloadType(), commandMessage.getPayload());
        unitOfWork.afterCommit(u -> {
            log.debug("Processed command: {}, Payload: {}", commandMessage.getPayloadType(), commandMessage.getPayload());
            DistributionSummary.builder("axon.command")
                    .tag("type", commandMessage.getPayloadType().getSimpleName())
                    .tag("status", "processed")
                    .register(meterRegistry)
                    .record(1);
        });
        return interceptorChain.proceed();
    }
}
