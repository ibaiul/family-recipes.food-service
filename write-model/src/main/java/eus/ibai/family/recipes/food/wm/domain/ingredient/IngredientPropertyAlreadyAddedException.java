package eus.ibai.family.recipes.food.wm.domain.ingredient;

public class IngredientPropertyAlreadyAddedException extends RuntimeException {

    public IngredientPropertyAlreadyAddedException(String message) {
        super(message);
    }
}
