package eus.ibai.family.recipes.food.rm.application.dto;

import eus.ibai.family.recipes.food.rm.domain.property.PropertyProjection;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record BasicPropertyDto(@NotNull String id, @NotEmpty String name) {

    public static BasicPropertyDto fromProjection(PropertyProjection property) {
        return new BasicPropertyDto(property.id(), property.name());
    }
}
