package eus.ibai.family.recipes.food.wm.domain.recipe;

import eus.ibai.family.recipes.food.wm.domain.command.AggregateCommand;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record AddRecipeTagCommand(@TargetAggregateIdentifier String aggregateId, String recipeTag) implements AggregateCommand<String> {}
