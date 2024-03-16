package eus.ibai.family.recipes.food.wm.domain.recipe;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class RecipeProperties {

    private ImageProperties images;

    record ImageProperties(String storagePath, Set<String> mediaTypes, long minSize, long maxSize) {}
}
