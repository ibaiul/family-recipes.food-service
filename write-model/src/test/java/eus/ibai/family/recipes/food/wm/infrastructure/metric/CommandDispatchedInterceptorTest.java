package eus.ibai.family.recipes.food.wm.infrastructure.metric;

import eus.ibai.family.recipes.food.wm.domain.recipe.CreateRecipeCommand;
import eus.ibai.family.recipes.food.wm.domain.recipe.DeleteRecipeCommand;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommandDispatchedInterceptorTest {

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private CommandDispatchedInterceptor interceptor;

    @ParameterizedTest
    @MethodSource
    void should_record_dispatched_commands(Object command) {
        GenericCommandMessage<?> axonMessage = new GenericCommandMessage<>(command);

        interceptor.handle(axonMessage);

        double dispatchedCommands = meterRegistry.get("axon.command")
                .tag("type", command.getClass().getSimpleName())
                .tag("status", "dispatched")
                .summary().count();
        assertThat(dispatchedCommands).isEqualTo(1);
    }

    private static Stream<?> should_record_dispatched_commands() {
        return Stream.of(
                new CreateRecipeCommand("id", "name"),
                new DeleteRecipeCommand("id"),
                new TestCommand()
        );
    }

    private record TestCommand() {}
}
