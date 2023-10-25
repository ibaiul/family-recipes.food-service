package eus.ibai.family.recipes.food.rm.infrastructure.metric;

import eus.ibai.family.recipes.food.event.RecipeTagAddedEvent;
import eus.ibai.family.recipes.food.event.RecipeTagRemovedEvent;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.RecipeEntityRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static eus.ibai.family.recipes.food.rm.infrastructure.config.AxonConfig.RECIPE_TAG_METRICS_EVENT_PROCESSOR;

@Component
@RequiredArgsConstructor
@ProcessingGroup(RECIPE_TAG_METRICS_EVENT_PROCESSOR)
public class RecipeTagStatsRecorder {

    @Autowired
    private final MeterRegistry meterRegistry;

    @Autowired
    private final RecipeEntityRepository recipeRepository;

    private final Map<String, AtomicLong> recipeTagCount = new ConcurrentHashMap<>();

    @PostConstruct
    void recordInitialStats() {
        recipeRepository.findAllTags()
                .groupBy(String::valueOf)
                .flatMap(group -> Mono.zip(Mono.just(group.key()), group.count()))
                .subscribe(group -> {
                    AtomicLong tagAmount = registerInitialValue(meterRegistry, group.getT1(), group.getT2());
                    recipeTagCount.put(group.getT1(), tagAmount);
                });
    }

    @EventHandler
    void on(RecipeTagAddedEvent event) {
        AtomicLong count = recipeTagCount.get(event.recipeTag());
        if (count == null) {
            count = registerInitialValue(meterRegistry, event.recipeTag(), 0);
            recipeTagCount.put(event.recipeTag(), count);
        }
        count.incrementAndGet();
    }

    @EventHandler
    void on(RecipeTagRemovedEvent event) {
        AtomicLong count = recipeTagCount.get(event.recipeTag());
        if (count != null) {
            count.decrementAndGet();
        }
    }

    private AtomicLong registerInitialValue(MeterRegistry meterRegistry, String tagName, long tagCount) {
        return meterRegistry.gauge("recipe.tags", List.of(Tag.of("name", tagName)), new AtomicLong(tagCount), AtomicLong::get);
    }
}
