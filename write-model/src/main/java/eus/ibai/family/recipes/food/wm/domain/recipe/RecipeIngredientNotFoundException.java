package eus.ibai.family.recipes.food.wm.domain.recipe;

public class RecipeIngredientNotFoundException extends RuntimeException {
    public RecipeIngredientNotFoundException(String message) {
        super(message);
    }
}
