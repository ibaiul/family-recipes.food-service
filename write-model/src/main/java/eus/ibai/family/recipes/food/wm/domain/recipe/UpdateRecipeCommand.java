package eus.ibai.family.recipes.food.wm.domain.recipe;

import eus.ibai.family.recipes.food.wm.domain.command.AggregateCommand;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Set;

public record UpdateRecipeCommand(@TargetAggregateIdentifier String aggregateId, String recipeName, Set<String> recipeLinks) implements AggregateCommand<String> {}
