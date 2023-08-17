package eus.ibai.family.recipes.food.wm.infrastructure.metric;

import eus.ibai.family.recipes.food.event.RecipeCreatedEvent;
import eus.ibai.family.recipes.food.event.RecipeDeletedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.axonframework.eventhandling.GenericEventMessage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EventDispatchedInterceptorTest {

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private EventDispatchedInterceptor interceptor;

    @ParameterizedTest
    @MethodSource
    void should_record_dispatched_events(Object event) {
        GenericEventMessage<?> axonMessage = new GenericEventMessage<>(event);

        interceptor.handle(axonMessage);

        double dispatchedCommands = meterRegistry.get("axon.event")
                .tag("type", event.getClass().getSimpleName())
                .tag("status", "dispatched")
                .summary().count();
        assertThat(dispatchedCommands).isEqualTo(1);
    }

    private static Stream<?> should_record_dispatched_events() {
        return Stream.of(
                new RecipeCreatedEvent("id", "name"),
                new RecipeDeletedEvent("id", "name"),
                new TestEvent()
        );
    }

    private record TestEvent() {}
}
