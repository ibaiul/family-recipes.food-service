package eus.ibai.family.recipes.food.wm.infrastructure.metric;

import eus.ibai.family.recipes.food.event.RecipeCreatedEvent;
import eus.ibai.family.recipes.food.event.RecipeDeletedEvent;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static eus.ibai.family.recipes.food.test.TestUtils.execute;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProcessedInterceptorTest {

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private EventProcessedInterceptor interceptor;

    @ParameterizedTest
    @MethodSource
    void should_record_processed_events(Object event) throws Exception {
        InterceptorChain mockInterceptorChain = mock(InterceptorChain.class);
        when(mockInterceptorChain.proceed()).thenReturn(null);
        GenericEventMessage<?> eventMessage = new GenericEventMessage<>(event);
        UnitOfWork<EventMessage<?>> unitOfWork = new DefaultUnitOfWork<>(eventMessage);

        execute(() -> {
            unitOfWork.start();
            interceptor.handle(unitOfWork, mockInterceptorChain);
            unitOfWork.commit();
        });

        await().atMost(1, SECONDS).untilAsserted(() -> {
            DistributionSummary metric = meterRegistry.find("axon.event")
                    .tag("type", event.getClass().getSimpleName())
                    .tag("status", "processed")
                    .summary();
            assertThat(metric).isNotNull();
            assertThat(metric.count()).isEqualTo(1);
        });
    }

    @Test
    void should_not_record_processed_events_when_processing_does_not_succeed() throws Exception {
        InterceptorChain mockInterceptorChain = mock(InterceptorChain.class);
        when(mockInterceptorChain.proceed()).thenThrow(new Exception());
        TestEvent event = new TestEvent();
        GenericEventMessage<TestEvent> eventMessage = new GenericEventMessage<>(event);
        UnitOfWork<EventMessage<?>> unitOfWork = new DefaultUnitOfWork<>(eventMessage);
        unitOfWork.start();

        assertThrows(Exception.class, () -> interceptor.handle(unitOfWork, mockInterceptorChain));

        verify(mockInterceptorChain).proceed();
        await().during(1, SECONDS).untilAsserted(() -> {
            DistributionSummary metric = meterRegistry.find("axon.event").tag("type", event.getClass().getSimpleName()).summary();
            assertThat(metric).isNull();
        });
    }


    @Test
    void should_not_record_processed_events_when_unit_of_work_does_not_commit() throws Exception {
        InterceptorChain mockInterceptorChain = mock(InterceptorChain.class);
        TestEvent event = new TestEvent();
        GenericEventMessage<TestEvent> eventMessage = new GenericEventMessage<>(event);
        UnitOfWork<EventMessage<?>> unitOfWork = new DefaultUnitOfWork<>(eventMessage);
        unitOfWork.start();

        interceptor.handle(unitOfWork, mockInterceptorChain);

        verify(mockInterceptorChain).proceed();
        await().during(1, SECONDS).untilAsserted(() -> {
            DistributionSummary metric = meterRegistry.find("axon.event").tag("type", event.getClass().getSimpleName()).summary();
            assertThat(metric).isNull();
        });
    }

    private static Stream<Object> should_record_processed_events() {
        return Stream.of(
                new RecipeCreatedEvent("id", "name"),
                new RecipeDeletedEvent("id", "name"),
                new TestEvent()
        );
    }

    private record TestEvent() {}
}
