package eus.ibai.family.recipes.food.rm.infrastructure.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.util.HashSet;
import java.util.Set;

@Data
@Table("ingredient")
@NoArgsConstructor
public class IngredientEntity {

    @Id
    private String id;

    private String name;

    @Transient
    private Set<PropertyEntity> properties;

    public IngredientEntity(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public IngredientEntity addProperty(PropertyEntity property) {
        if (properties == null) {
            properties = new HashSet<>();
        }
        properties.add(property);
        return this;
    }
}
