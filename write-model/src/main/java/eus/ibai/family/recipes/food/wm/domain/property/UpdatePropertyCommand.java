package eus.ibai.family.recipes.food.wm.domain.property;

import eus.ibai.family.recipes.food.wm.domain.command.AggregateCommand;
import jakarta.validation.constraints.NotBlank;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record UpdatePropertyCommand(@NotBlank @TargetAggregateIdentifier String aggregateId, @NotBlank String propertyName) implements AggregateCommand<String> {}
