package eus.ibai.family.recipes.food.health;

import org.springframework.boot.actuate.health.Health;
import reactor.core.publisher.Mono;

public interface ComponentHealthContributor {

    String getComponentName();

    long getInterval();

    Mono<Health> doHealthCheck();
}
