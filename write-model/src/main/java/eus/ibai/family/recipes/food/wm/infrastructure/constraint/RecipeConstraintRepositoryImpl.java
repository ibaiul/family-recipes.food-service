package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import eus.ibai.family.recipes.food.wm.domain.recipe.RecipeConstraintRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
public class RecipeConstraintRepositoryImpl implements RecipeConstraintRepository {

    private RecipeNameConstraintRepository recipeNameConstraintRepository;

    private IngredientNameConstraintRepository ingredientNameConstraintRepository;

    @Override
    public boolean idExists(String recipeId) {
        return recipeNameConstraintRepository.existsById(recipeId);
    }

    @Override
    public boolean nameExists(String recipeName) {
        return recipeNameConstraintRepository.nameExists(recipeName);
    }

    @Override
    public boolean anotherRecipeContainsName(String recipeId, String recipeName) {
        return recipeNameConstraintRepository.nameExistsForAnotherRecipe(recipeId, recipeName);
    }

    @Override
    public Optional<String> retrieveIngredientId(String ingredientName) {
        return ingredientNameConstraintRepository.findByIngredientName(ingredientName)
                .map(IngredientNameConstraintEntity::getIngredientId);
    }
}
