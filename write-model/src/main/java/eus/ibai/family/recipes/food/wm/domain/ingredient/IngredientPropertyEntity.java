package eus.ibai.family.recipes.food.wm.domain.ingredient;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.axonframework.modelling.command.EntityId;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class IngredientPropertyEntity {

    @EntityId
    private String propertyId;

    private LocalDateTime addedOn;
}