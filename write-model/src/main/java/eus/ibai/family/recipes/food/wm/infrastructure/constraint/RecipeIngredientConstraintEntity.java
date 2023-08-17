package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(RecipeIngredientId.class)
@Table(name = "recipe_ingredient_constraint")
public class RecipeIngredientConstraintEntity {

    @Id
    @Column(name = "recipe_id")
    private String recipeId;

    @Id
    @Column(name = "ingredient_id")
    private String ingredientId;
}
