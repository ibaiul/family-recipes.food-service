package eus.ibai.family.recipes.food.wm.domain.recipe;

import eus.ibai.family.recipes.food.wm.domain.command.AggregateCommand;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record AddRecipeImageCommand(@TargetAggregateIdentifier String aggregateId, String imageId) implements AggregateCommand<String> {}
