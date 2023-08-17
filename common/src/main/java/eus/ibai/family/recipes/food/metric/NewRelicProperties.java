package eus.ibai.family.recipes.food.metric;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "newrelic")
public class NewRelicProperties {

    private boolean enabled;

    private App app;

    private Metrics metrics;

    private Keys keys;

    record App(String name) {}

    record Metrics(int step, String ingestUri) {}

    record Keys(String license) {}
}
