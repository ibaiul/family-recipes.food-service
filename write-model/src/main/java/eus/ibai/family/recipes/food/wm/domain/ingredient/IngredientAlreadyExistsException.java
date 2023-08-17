package eus.ibai.family.recipes.food.wm.domain.ingredient;

public class IngredientAlreadyExistsException extends Exception {

    public IngredientAlreadyExistsException(String message) {
        super(message);
    }
}
