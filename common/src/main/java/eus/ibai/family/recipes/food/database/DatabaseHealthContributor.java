package eus.ibai.family.recipes.food.database;

import eus.ibai.family.recipes.food.health.ComponentHealthContributor;
import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.r2dbc.ConnectionFactoryHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "management.health.database", name = "enabled", havingValue = "true")
public class DatabaseHealthContributor implements ComponentHealthContributor {

    private static final String COMPONENT_NAME = "database";

    private final ConnectionFactoryHealthIndicator decoratedIndicator;

    private final long interval;

    public DatabaseHealthContributor(ConnectionFactory connectionFactory, @Value("${management.health.database.interval:300}") long interval) {
        this.decoratedIndicator = new ConnectionFactoryHealthIndicator(connectionFactory);
        this.interval = interval;
    }

    @Override
    public String getComponentName() {
        return COMPONENT_NAME;
    }

    @Override
    public Mono<Health> doHealthCheck() {
        return decoratedIndicator.health()
                .map(health -> Health.status(health.getStatus()).build())
                .doOnNext(health -> log.trace("Received database health response: {}", health.getStatus()));
    }

    @Override
    public long getInterval() {
        return interval;
    }
}
