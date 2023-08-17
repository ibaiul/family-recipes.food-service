package eus.ibai.family.recipes.food.wm.infrastructure.metric;

import eus.ibai.family.recipes.food.wm.domain.recipe.CreateRecipeCommand;
import eus.ibai.family.recipes.food.wm.domain.recipe.DeleteRecipeCommand;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
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
class CommandProcessedInterceptorTest {

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private CommandProcessedInterceptor interceptor;

    @ParameterizedTest
    @MethodSource
    void should_record_processed_commands(Object command) throws Exception {
        InterceptorChain mockInterceptorChain = mock(InterceptorChain.class);
        when(mockInterceptorChain.proceed()).thenReturn(null);
        GenericCommandMessage<?> commandMessage = new GenericCommandMessage<>(command);
        UnitOfWork<CommandMessage<?>> unitOfWork = new DefaultUnitOfWork<>(commandMessage);

        execute(() -> {
            unitOfWork.start();
            interceptor.handle(unitOfWork, mockInterceptorChain);
            unitOfWork.commit();
        });

        await().atMost(1, SECONDS).untilAsserted(() -> {
            DistributionSummary metric = meterRegistry.find("axon.command").tag("type", command.getClass().getSimpleName()).summary();
            assertThat(metric).isNotNull();
            assertThat(metric.count()).isEqualTo(1);
        });
    }

    @Test
    void should_not_record_processed_events_when_processing_does_not_succeed() throws Exception {
        InterceptorChain mockInterceptorChain = mock(InterceptorChain.class);
        when(mockInterceptorChain.proceed()).thenThrow(new Exception());
        TestCommand command = new TestCommand();
        GenericCommandMessage<TestCommand> commandMessage = new GenericCommandMessage<>(command);
        UnitOfWork<CommandMessage<?>> unitOfWork = new DefaultUnitOfWork<>(commandMessage);
        unitOfWork.start();

        assertThrows(Exception.class, () -> interceptor.handle(unitOfWork, mockInterceptorChain));

        await().during(1, SECONDS).untilAsserted(() -> {
            DistributionSummary metric = meterRegistry.find("axon.command").tag("type", command.getClass().getSimpleName()).summary();
            assertThat(metric).isNull();
        });
    }


    @Test
    void should_not_record_processed_events_when_unit_of_work_does_not_commit() throws Exception {
        InterceptorChain mockInterceptorChain = mock(InterceptorChain.class);
        TestCommand event = new TestCommand();
        GenericCommandMessage<TestCommand> commandMessage = new GenericCommandMessage<>(event);
        UnitOfWork<CommandMessage<?>> unitOfWork = new DefaultUnitOfWork<>(commandMessage);
        unitOfWork.start();

        interceptor.handle(unitOfWork, mockInterceptorChain);

        verify(mockInterceptorChain).proceed();
        await().during(1, SECONDS).untilAsserted(() -> {
            DistributionSummary metric = meterRegistry.find("axon.command")
                    .tag("type", event.getClass().getSimpleName())
                    .tag("status", "processed")
                    .summary();
            assertThat(metric).isNull();
        });
    }

    private static Stream<Object> should_record_processed_commands() {
        return Stream.of(
                new CreateRecipeCommand("id", "name"),
                new DeleteRecipeCommand("id"),
                new TestCommand()
        );
    }

    private record TestCommand() {}
}
