package eus.ibai.family.recipes.food.rm.infrastructure.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Table("recipe_ingredient")
@NoArgsConstructor
public class RecipeIngredientEntity {

    @Column("recipe_id")
    private String recipeId;

    @Column("ingredient_id")
    private String ingredientId;

    @CreatedDate
    @Column("added_on")
    private LocalDateTime addedOn;
}
