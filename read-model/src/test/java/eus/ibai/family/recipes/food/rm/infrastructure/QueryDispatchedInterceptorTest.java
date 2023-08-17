package eus.ibai.family.recipes.food.rm.infrastructure;

import eus.ibai.family.recipes.food.rm.domain.property.FindAllPropertiesQuery;
import eus.ibai.family.recipes.food.rm.domain.recipe.FindRecipesByQuery;
import eus.ibai.family.recipes.food.rm.infrastructure.metric.QueryDispatchedInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.GenericQueryMessage;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class QueryDispatchedInterceptorTest {

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private QueryDispatchedInterceptor interceptor;

    @ParameterizedTest
    @MethodSource
    void should_record_dispatched_queries(Object query) {
        GenericQueryMessage<?, ?> axonMessage = new GenericQueryMessage<>(query, ResponseTypes.instanceOf(String.class));

        interceptor.handle(axonMessage);

        double dispatchedCommands = meterRegistry.get("axon.query")
                .tag("type", query.getClass().getSimpleName())
                .tag("status", "dispatched")
                .summary().count();
        assertThat(dispatchedCommands).isEqualTo(1);
    }

    private static Stream<?> should_record_dispatched_queries() {
        return Stream.of(
                new FindRecipesByQuery("id", null),
                new FindAllPropertiesQuery(),
                new TestQuery()
        );
    }

    private record TestQuery() {}
}
