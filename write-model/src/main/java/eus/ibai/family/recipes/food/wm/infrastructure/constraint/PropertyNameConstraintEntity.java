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
@Table(name = "property_name_constraint")
@NoArgsConstructor
@AllArgsConstructor
public class PropertyNameConstraintEntity {

    @Id
    @Column(name = "property_id")
    private String propertyId;

    @Column(name = "property_name", unique = true)
    private String propertyName;
}
