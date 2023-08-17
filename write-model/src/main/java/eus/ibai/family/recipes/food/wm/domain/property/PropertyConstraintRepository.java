package eus.ibai.family.recipes.food.wm.domain.property;

public interface PropertyConstraintRepository {

    boolean nameExists(String propertyName);

    boolean anotherPropertyContainsName(String propertyId, String propertyName);

    boolean isPropertyBoundToIngredients(String propertyId);
}
