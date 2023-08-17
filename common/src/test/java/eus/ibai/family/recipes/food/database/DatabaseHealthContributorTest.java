package eus.ibai.family.recipes.food.database;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import reactor.test.StepVerifier;

import static eus.ibai.family.recipes.food.test.TestData.mockConnectionFactoryHealthUp;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseHealthContributorTest {

    @Mock
    private ConnectionFactory connectionFactory;

    private DatabaseHealthContributor healthContributor;

    @BeforeEach
    void beforeEach() {
        healthContributor = new DatabaseHealthContributor(connectionFactory, 60);
    }

    @Test
    void should_return_healthy_when_database_available() {
        mockConnectionFactoryHealthUp(connectionFactory);
        Health expectedHealth = Health.up().build();

        StepVerifier.create(healthContributor.doHealthCheck())
                .expectNext(expectedHealth)
                .verifyComplete();
    }

    @Test
    void should_return_unhealthy_when_database_unavailable() {
        when(connectionFactory.getMetadata()).thenThrow(new RuntimeException(""));
        Health expectedHealth = Health.down().build();

        StepVerifier.create(healthContributor.doHealthCheck())
                .expectNext(expectedHealth)
                .verifyComplete();
    }
}
