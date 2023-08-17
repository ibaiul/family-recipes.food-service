package eus.ibai.family.recipes.food.wm.domain.recipe;

public class RecipeAlreadyExistsException extends Exception {

    public RecipeAlreadyExistsException(String message) {
        super(message);
    }
}
