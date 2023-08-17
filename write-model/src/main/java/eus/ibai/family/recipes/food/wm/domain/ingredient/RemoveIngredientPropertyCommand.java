package eus.ibai.family.recipes.food.wm.domain.ingredient;

import eus.ibai.family.recipes.food.wm.domain.command.AggregateCommand;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record RemoveIngredientPropertyCommand(@TargetAggregateIdentifier String aggregateId, String propertyId) implements AggregateCommand<String> {}
