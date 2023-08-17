package eus.ibai.family.recipes.food.wm.domain.recipe;

import eus.ibai.family.recipes.food.wm.domain.command.AggregateCommand;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record AddRecipeIngredientCommand(@TargetAggregateIdentifier String aggregateId, String ingredientId) implements AggregateCommand<String> {}
