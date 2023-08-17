package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "recipe_name_constraint")
@NoArgsConstructor
@AllArgsConstructor
public class RecipeNameConstraintEntity {

    @Id
    @Column(name = "recipe_id")
    private String recipeId;

    @Column(name = "recipe_name", unique = true)
    private String recipeName;
}
