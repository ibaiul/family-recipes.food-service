package eus.ibai.family.recipes.food.wm.infrastructure.metric;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.MessageDispatchInterceptor;

import java.util.List;
import java.util.function.BiFunction;

@Slf4j
@RequiredArgsConstructor
public class CommandDispatchedInterceptor implements MessageDispatchInterceptor<CommandMessage<?>> {

    private final MeterRegistry meterRegistry;

    @Nonnull
    @Override
    public BiFunction<Integer, CommandMessage<?>, CommandMessage<?>> handle(@Nonnull List<? extends CommandMessage<?>> messages) {
        return (index, commandMessage) -> {
            log.debug("Command received: {}, Payload: {}", commandMessage.getPayloadType(), commandMessage.getPayload());
            DistributionSummary.builder("axon.command")
                    .tag("type", commandMessage.getPayloadType().getSimpleName())
                    .tag("status", "dispatched")
                    .register(meterRegistry)
                    .record(1);
            return commandMessage;
        };
    }
}
