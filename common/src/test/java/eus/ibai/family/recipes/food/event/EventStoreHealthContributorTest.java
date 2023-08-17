package eus.ibai.family.recipes.food.event;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import reactor.test.StepVerifier;

import javax.sql.DataSource;
import java.sql.SQLException;

import static eus.ibai.family.recipes.food.test.TestData.mockDatasourceHealthUp;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventStoreHealthContributorTest {

    @Mock
    private DataSource dataSource;

    private EventStoreHealthContributor healthContributor;

    @BeforeEach
    void beforeEach() {
        healthContributor = new EventStoreHealthContributor(dataSource, 60);
    }

    @Test
    void should_return_healthy_when_database_available() throws SQLException {
        mockDatasourceHealthUp(dataSource);
        Health expectedHealth = Health.up().build();

        StepVerifier.create(healthContributor.doHealthCheck())
                .expectNext(expectedHealth)
                .verifyComplete();
    }

    @Test
    void should_return_unhealthy_when_database_unavailable() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException(""));
        Health expectedHealth = Health.down().build();

        StepVerifier.create(healthContributor.doHealthCheck())
                .expectNext(expectedHealth)
                .verifyComplete();
    }
}
