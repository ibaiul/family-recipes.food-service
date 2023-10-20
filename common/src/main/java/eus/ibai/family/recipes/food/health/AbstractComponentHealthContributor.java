package eus.ibai.family.recipes.food.health;

import java.util.Objects;

public abstract class AbstractComponentHealthContributor implements ComponentHealthContributor {

    private final String componentName;

    private final long interval;

    protected AbstractComponentHealthContributor(String componentName, long interval) {
        this.componentName = Objects.requireNonNull(componentName);
        this.interval = interval;
    }

    @Override
    public String getComponentName() {
        return componentName;
    }

    @Override
    public long getInterval() {
        return interval;
    }
}
