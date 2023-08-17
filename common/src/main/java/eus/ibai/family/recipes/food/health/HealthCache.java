package eus.ibai.family.recipes.food.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class HealthCache {

    private final Map<String, Health> lastKnownHealths = new ConcurrentHashMap<>();

    public Health getHealth(String componentName) {
        return Optional.ofNullable(lastKnownHealths.get(componentName))
                .orElse(Health.unknown().build());
    }

    public void setHealth(String componentName, Health health) {
        lastKnownHealths.put(componentName, health);
    }
}
