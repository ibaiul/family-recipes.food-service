package eus.ibai.family.recipes.food.rm.test;

import eus.ibai.family.recipes.food.rm.infrastructure.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import reactor.test.StepVerifier;

import static org.springframework.test.context.junit.jupiter.SpringExtension.getApplicationContext;

@Slf4j
public class DataCleanupExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        log.debug("After each");
        IngredientPropertyEntityRepository ingredientPropertyEntityRepository = getApplicationContext(context).getBean(IngredientPropertyEntityRepository.class);
        ingredientPropertyEntityRepository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();
        RecipeIngredientEntityRepository recipeIngredientEntityRepository = getApplicationContext(context).getBean(RecipeIngredientEntityRepository.class);
        recipeIngredientEntityRepository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();
        PropertyEntityRepository propertyEntityRepository = getApplicationContext(context).getBean(PropertyEntityRepository.class);
        propertyEntityRepository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();
        IngredientEntityRepository ingredientEntityRepository = getApplicationContext(context).getBean(IngredientEntityRepository.class);
        ingredientEntityRepository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();
        RecipeEntityRepository recipeEntityRepository = getApplicationContext(context).getBean(RecipeEntityRepository.class);
        recipeEntityRepository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();
    }
}
