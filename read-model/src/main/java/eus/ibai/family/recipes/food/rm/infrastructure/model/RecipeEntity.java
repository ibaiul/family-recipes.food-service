package eus.ibai.family.recipes.food.rm.infrastructure.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.util.HashSet;
import java.util.Set;

@Data
@Table("recipe")
@NoArgsConstructor
public class RecipeEntity {

    @Id
    private String id;

    private String name;

    private Set<String> links;

    @Transient
    private Set<IngredientEntity> ingredients;

    private Set<String> tags;

    public RecipeEntity(String id, String name) {
        this(id, name, null);
    }

    public RecipeEntity(String id, String name, Set<String> links) {
        this.id = id;
        this.name = name;
        this.links = links;
    }

    public RecipeEntity addIngredient(IngredientEntity ingredient) {
        if (ingredients == null) {
            ingredients = new HashSet<>();
        }
        ingredients.add(ingredient);
        return this;
    }

    public RecipeEntity addTag(String tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        tags.add(tag);
        return this;
    }

    public RecipeEntity removeTag(String tag) {
        tags.remove(tag);
        return this;
    }
}
