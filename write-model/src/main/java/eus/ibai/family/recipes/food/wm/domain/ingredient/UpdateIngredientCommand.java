package eus.ibai.family.recipes.food.wm.domain.ingredient;

import eus.ibai.family.recipes.food.wm.domain.command.AggregateCommand;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record UpdateIngredientCommand(@TargetAggregateIdentifier String aggregateId, String ingredientName) implements AggregateCommand<String> {}
