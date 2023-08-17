package eus.ibai.family.recipes.food.rm.infrastructure.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("property")
@AllArgsConstructor
public class PropertyEntity {

    @Id
    private String id;

    private String name;
}
