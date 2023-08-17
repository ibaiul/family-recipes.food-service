package eus.ibai.family.recipes.food.rm.infrastructure.model;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Table("ingredient_property")
public class IngredientPropertyEntity {

    @Column("ingredient_id")
    private String ingredientId;

    @Column("property_id")
    private String propertyId;

    @CreatedDate
    @Column("added_on")
    private LocalDateTime addedOn;
}
