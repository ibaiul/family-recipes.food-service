package eus.ibai.family.recipes.food.wm.domain.property;

import eus.ibai.family.recipes.food.wm.domain.command.AggregateCommand;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record DeletePropertyCommand(@TargetAggregateIdentifier String aggregateId) implements AggregateCommand<String> {}
