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
@Table(name = "ingredient_name_constraint")
@NoArgsConstructor
@AllArgsConstructor
public class IngredientNameConstraintEntity {

    @Id
    @Column(name = "ingredient_id")
    private String ingredientId;

    @Column(name = "ingredient_name", unique = true)
    private String ingredientName;
}
