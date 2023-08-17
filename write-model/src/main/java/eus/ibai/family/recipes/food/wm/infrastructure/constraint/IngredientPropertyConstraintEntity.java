package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(IngredientPropertyId.class)
@Table(name = "ingredient_property_constraint")
public class IngredientPropertyConstraintEntity {

    @Id
    @Column(name = "ingredient_id")
    private String ingredientId;

    @Id
    @Column(name = "property_id")
    private String propertyId;
}


