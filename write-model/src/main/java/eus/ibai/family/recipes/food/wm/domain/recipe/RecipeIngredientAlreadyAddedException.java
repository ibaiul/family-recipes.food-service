package eus.ibai.family.recipes.food.wm.domain.recipe;

public class RecipeIngredientAlreadyAddedException extends RuntimeException {

    public RecipeIngredientAlreadyAddedException(String message) {
        super(message);
    }
}
