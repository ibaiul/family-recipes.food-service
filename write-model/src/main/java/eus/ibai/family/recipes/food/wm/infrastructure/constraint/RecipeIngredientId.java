package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientId implements Serializable {

    private String recipeId;

    private String ingredientId;
}
