package eus.ibai.family.recipes.food.event;

import eus.ibai.family.recipes.food.health.AbstractComponentHealthContributor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "management.health.event-store", name = "enabled", havingValue = "true")
public class EventStoreHealthContributor extends AbstractComponentHealthContributor {

    private final DataSourceHealthIndicator decoratedIndicator;

    public EventStoreHealthContributor(@Qualifier("event-store-db") DataSource dataSource, @Value("${management.health.event-store.interval:300}") long interval) {
        super("event-store", interval);
        this.decoratedIndicator = new DataSourceHealthIndicator(dataSource);
    }

    @Override
    public Mono<Health> doHealthCheck() {
        return Mono.just(decoratedIndicator.health())
                .map(health -> Health.status(health.getStatus()).build())
                .doOnNext(health -> log.trace("Received event store health response: {}", health.getStatus()));
    }
}
