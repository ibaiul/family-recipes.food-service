package eus.ibai.family.recipes.food.wm.domain.command;

public interface AggregateCommand<T> {

    T aggregateId();
}
