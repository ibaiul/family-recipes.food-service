package eus.ibai.family.recipes.food.wm.infrastructure.constraint;

import eus.ibai.family.recipes.food.wm.domain.property.PropertyConstraintRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PropertyConstraintRepositoryImpl implements PropertyConstraintRepository {

    private final PropertyNameConstraintRepository propertyNameConstraintRepository;

    private final IngredientPropertyConstraintRepository ingredientPropertyConstraintRepository;

    @Override
    public boolean nameExists(String propertyName) {
        return propertyNameConstraintRepository.nameExists(propertyName);
    }

    @Override
    public boolean anotherPropertyContainsName(String propertyId, String propertyName) {
        return propertyNameConstraintRepository.nameExistsForAnotherProperty(propertyId, propertyName);
    }

    @Override
    public boolean isPropertyBoundToIngredients(String propertyId) {
        return ingredientPropertyConstraintRepository.propertyExists(propertyId);
    }
}
