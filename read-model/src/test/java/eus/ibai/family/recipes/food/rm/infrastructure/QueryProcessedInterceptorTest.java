package eus.ibai.family.recipes.food.rm.infrastructure;

import eus.ibai.family.recipes.food.rm.domain.property.FindAllPropertiesQuery;
import eus.ibai.family.recipes.food.rm.domain.recipe.FindRecipeByIdQuery;
import eus.ibai.family.recipes.food.rm.infrastructure.metric.QueryProcessedInterceptor;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.queryhandling.GenericQueryMessage;
import org.axonframework.queryhandling.QueryMessage;
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
class QueryProcessedInterceptorTest {

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private QueryProcessedInterceptor interceptor;

    @ParameterizedTest
    @MethodSource
    void should_record_processed_queries(Object query) throws Exception {
        InterceptorChain mockInterceptorChain = mock(InterceptorChain.class);
        when(mockInterceptorChain.proceed()).thenReturn(null);
        GenericQueryMessage<?, ?> queryMessage = new GenericQueryMessage<>(query, ResponseTypes.instanceOf(String.class));
        UnitOfWork<QueryMessage<?, ?>> unitOfWork = new DefaultUnitOfWork<>(queryMessage);

        execute(() -> {
            unitOfWork.start();
            interceptor.handle(unitOfWork, mockInterceptorChain);
            unitOfWork.commit();
        });

        await().atMost(1, SECONDS).untilAsserted(() -> {
            DistributionSummary metric = meterRegistry.find("axon.query")
                    .tag("type", query.getClass().getSimpleName())
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
        TestQuery query = new TestQuery();
        GenericQueryMessage<TestQuery, String> queryMessage = new GenericQueryMessage<>(query, ResponseTypes.instanceOf(String.class));
        UnitOfWork<QueryMessage<?, ?>> unitOfWork = new DefaultUnitOfWork<>(queryMessage);
        unitOfWork.start();

        assertThrows(Exception.class, () -> interceptor.handle(unitOfWork, mockInterceptorChain));

        await().during(1, SECONDS).untilAsserted(() -> {
            DistributionSummary metric = meterRegistry.find("axon.query").tag("type", query.getClass().getSimpleName()).summary();
            assertThat(metric).isNull();
        });
    }


    @Test
    void should_not_record_processed_events_when_unit_of_work_does_not_commit() throws Exception {
        InterceptorChain mockInterceptorChain = mock(InterceptorChain.class);
        TestQuery event = new TestQuery();
        GenericQueryMessage<TestQuery, String> queryMessage = new GenericQueryMessage<>(event, ResponseTypes.instanceOf(String.class));
        UnitOfWork<QueryMessage<?, ?>> unitOfWork = new DefaultUnitOfWork<>(queryMessage);
        unitOfWork.start();

        interceptor.handle(unitOfWork, mockInterceptorChain);

        verify(mockInterceptorChain).proceed();
        await().during(1, SECONDS).untilAsserted(() -> {
            DistributionSummary metric = meterRegistry.find("axon.query").tag("type", event.getClass().getSimpleName()).summary();
            assertThat(metric).isNull();
        });
    }

    private static Stream<Object> should_record_processed_queries() {
        return Stream.of(
                new FindRecipeByIdQuery("id"),
                new FindAllPropertiesQuery(),
                new TestQuery()
        );
    }

    private record TestQuery() {}
}
