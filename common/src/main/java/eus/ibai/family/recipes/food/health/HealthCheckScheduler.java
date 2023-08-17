package eus.ibai.family.recipes.food.health;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Component
@EnableAsync
public class HealthCheckScheduler {

    private final HealthCache healthCache;

    private final List<ComponentHealthContributor> componentHealthContributors;

    private final ScheduledExecutorService executorService;

    private final long initialDelay;

    public HealthCheckScheduler(HealthCache healthCache, List<ComponentHealthContributor> componentHealthContributors, @Value("${management.health.initial-delay:-1}") long initialDelay) {
        this.healthCache = healthCache;
        this.componentHealthContributors = componentHealthContributors;
        this.executorService = Executors.newScheduledThreadPool(2);
        this.initialDelay = initialDelay;
    }

    @PostConstruct
    public void init() {
        componentHealthContributors.forEach(contributor -> {
            healthCache.setHealth(contributor.getComponentName(), Health.up().build());
            executorService.scheduleWithFixedDelay(() -> checkComponentHealth(contributor.getComponentName()), initialDelay, contributor.getInterval(), SECONDS);
        });
    }

    private void checkComponentHealth(String componentName) {
        Flux.fromIterable(componentHealthContributors)
                .filter(componentHealthContributor -> componentName.equals(componentHealthContributor.getComponentName()))
                .doOnNext(healthContributor -> log.trace("Checking health of {} component.", healthContributor.getComponentName()))
                .flatMap(componentHealthContributor -> componentHealthContributor.doHealthCheck()
                        .map(health -> Tuples.of(componentHealthContributor.getComponentName(), health)))
                .doOnNext(componentTuple -> log.trace("Updating health cache entry: {} -> {}", componentTuple.getT1(), componentTuple.getT2().getStatus()))
                .subscribe(componentTuple -> healthCache.setHealth(componentTuple.getT1(), componentTuple.getT2()));
    }
}
