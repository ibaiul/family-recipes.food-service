package eus.ibai.family.recipes.food.wm.domain.recipe;

import eus.ibai.family.recipes.food.event.*;
import org.axonframework.eventsourcing.AggregateDeletedException;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static eus.ibai.family.recipes.food.test.TestUtils.FIXED_CLOCK;
import static eus.ibai.family.recipes.food.test.TestUtils.fixedTime;
import static org.assertj.core.api.Assertions.assertThat;

class RecipeAggregateTest {

    private FixtureConfiguration<RecipeAggregate> fixture;

    @BeforeEach
    public void setUp() {
        fixture = new AggregateTestFixture<>(RecipeAggregate.class);
        fixture.registerInjectableResource(FIXED_CLOCK);
    }

    @Test
    void should_create_recipe() {
        fixture.givenNoPriorActivity()
                .when(new CreateRecipeCommand("recipeId", "Pasta carbonara"))
                .expectEvents(new RecipeCreatedEvent("recipeId", "Pasta carbonara"))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo("recipeId");
                    assertThat(state.getName()).isEqualTo("Pasta carbonara");
                    assertThat(state.getIngredients()).isEmpty();
                });
    }

    @Test
    void should_update_recipe() {
        fixture.given(new RecipeCreatedEvent("recipeId", "Pasta carbonara"))
                .when(new UpdateRecipeCommand("recipeId", "Spaghetti carbonara", Set.of("https://pasta.com")))
                .expectEvents(new RecipeUpdatedEvent("recipeId", "Spaghetti carbonara", Set.of("https://pasta.com")))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo("recipeId");
                    assertThat(state.getName()).isEqualTo("Spaghetti carbonara");
                    assertThat(state.getLinks()).containsExactly("https://pasta.com");
                    assertThat(state.getIngredients()).isEmpty();
                });
    }

    @Test
    void should_not_update_recipe_when_not_changed() {
        fixture.given(new RecipeCreatedEvent("recipeId", "Pasta carbonara"),
                        new RecipeUpdatedEvent("recipeId", "Pasta carbonara", Set.of("https://pasta.com")))
                .when(new UpdateRecipeCommand("recipeId", "Pasta carbonara", Set.of("https://pasta.com")))
                .expectNoEvents();
    }

    @Test
    void should_delete_recipe() {
        fixture.given(new RecipeCreatedEvent("recipeId", "Pasta carbonara"))
                .when(new DeleteRecipeCommand("recipeId"))
                .expectEvents(new RecipeDeletedEvent("recipeId", "Pasta carbonara"));
    }

    @Test
    void should_not_accept_commands_when_recipe_is_deleted() {
        fixture.given(new RecipeCreatedEvent("recipeId", "Pasta carbonara"),
                        new RecipeDeletedEvent("recipeId", "Pasta carbonara"))
                .when(new UpdateRecipeCommand("recipeId", "Spaghetti carbonara", Collections.emptySet()))
                .expectException(AggregateDeletedException.class)
                .expectNoEvents();
    }

    @Test
    void should_add_recipe_ingredient() {
        RecipeIngredient recipeIngredient = new RecipeIngredient("ingredientId", fixedTime());
        RecipeIngredientEntity expectedRecipeIngredient = new RecipeIngredientEntity(recipeIngredient.ingredientId(), recipeIngredient.addedOn());

        fixture.given(new RecipeCreatedEvent("recipeId", "Pasta carbonara"))
                .when(new AddRecipeIngredientCommand("recipeId", "ingredientId"))
                .expectEvents(new RecipeIngredientAddedEvent("recipeId", recipeIngredient))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo("recipeId");
                    assertThat(state.getName()).isEqualTo("Pasta carbonara");
                    assertThat(state.getIngredients()).containsExactly(expectedRecipeIngredient);
                });
    }

    @Test
    void should_not_add_recipe_ingredient_if_already_added() {
        RecipeIngredient recipeIngredient = new RecipeIngredient("ingredientId", fixedTime());

        fixture.given(new RecipeCreatedEvent("recipeId", "Pasta carbonara"),
                        new RecipeIngredientAddedEvent("recipeId", recipeIngredient))
                .when(new AddRecipeIngredientCommand("recipeId", "ingredientId"))
                .expectException(RecipeIngredientAlreadyAddedException.class)
                .expectNoEvents();
    }

    @Test
    void should_remove_recipe_ingredient_when_present() {
        RecipeIngredient recipeIngredient = new RecipeIngredient("ingredientId", fixedTime());

        fixture.given(new RecipeCreatedEvent("recipeId", "Pasta carbonara"),
                        new RecipeIngredientAddedEvent("recipeId", recipeIngredient))
                .when(new RemoveRecipeIngredientCommand("recipeId", "ingredientId"))
                .expectEvents(new RecipeIngredientRemovedEvent("recipeId", recipeIngredient))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo("recipeId");
                    assertThat(state.getName()).isEqualTo("Pasta carbonara");
                    assertThat(state.getIngredients()).isEmpty();
                });
    }

    @Test
    void should_not_remove_recipe_ingredient_when_not_present() {
        fixture.given(new RecipeCreatedEvent("recipeId", "Pasta carbonara"))
                .when(new RemoveRecipeIngredientCommand("recipeId", "ingredientId"))
                .expectException(RecipeIngredientNotFoundException.class)
                .expectNoEvents();
    }
}
