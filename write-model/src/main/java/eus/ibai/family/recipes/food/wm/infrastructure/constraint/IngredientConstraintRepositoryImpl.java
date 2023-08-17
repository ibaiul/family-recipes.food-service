package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import eus.ibai.family.recipes.food.wm.domain.ingredient.IngredientConstraintRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
public class IngredientConstraintRepositoryImpl implements IngredientConstraintRepository {

    private RecipeIngredientConstraintRepository recipeIngredientConstraintRepository;

    private final IngredientNameConstraintRepository ingredientNameConstraintRepository;

    private final PropertyNameConstraintRepository propertyNameConstraintRepository;

    @Override
    public boolean nameExists(String ingredientName) {
        return ingredientNameConstraintRepository.nameExists(ingredientName);
    }

    @Override
    public boolean anotherIngredientContainsName(String ingredientId, String ingredientName) {
        return ingredientNameConstraintRepository.nameExistsForAnotherIngredient(ingredientId, ingredientName);
    }

    @Override
    public boolean isIngredientBoundToRecipes(String ingredientId) {
        return recipeIngredientConstraintRepository.ingredientExists(ingredientId);
    }

    @Override
    public Optional<String> retrievePropertyId(String propertyName) {
        return propertyNameConstraintRepository.findByPropertyName(propertyName)
                .map(PropertyNameConstraintEntity::getPropertyId);
    }
}
