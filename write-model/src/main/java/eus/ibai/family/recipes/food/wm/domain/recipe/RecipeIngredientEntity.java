package eus.ibai.family.recipes.food.wm.domain.recipe;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.axonframework.modelling.command.EntityId;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RecipeIngredientEntity {

    @EntityId
    private String ingredientId;

    private LocalDateTime addedOn;
}