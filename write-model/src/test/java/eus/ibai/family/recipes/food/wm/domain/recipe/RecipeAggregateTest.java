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

    @Test
    void should_add_recipe_tag() {
        String recipeId = "recipeId";
        String expectedTag = "First course";

        fixture.given(new RecipeCreatedEvent(recipeId, "Pasta carbonara"))
                .when(new AddRecipeTagCommand(recipeId, expectedTag))
                .expectEvents(new RecipeTagAddedEvent(recipeId, expectedTag))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo(recipeId);
                    assertThat(state.getName()).isEqualTo("Pasta carbonara");
                    assertThat(state.getTags()).containsExactly(expectedTag);
                });
    }

    @Test
    void should_not_add_recipe_tag_if_already_added() {
        String recipeId = "recipeId";
        String tag = "First course";

        fixture.given(new RecipeCreatedEvent(recipeId, "Pasta carbonara"),
                        new RecipeTagAddedEvent(recipeId, tag))
                .when(new AddRecipeTagCommand(recipeId, tag))
                .expectNoEvents();
    }

    @Test
    void should_remove_recipe_tag_when_present() {
        String recipeId = "recipeId";
        String tag = "First course";

        fixture.given(new RecipeCreatedEvent(recipeId, "Pasta carbonara"),
                        new RecipeTagAddedEvent(recipeId, tag))
                .when(new RemoveRecipeTagCommand(recipeId, tag))
                .expectEvents(new RecipeTagRemovedEvent(recipeId, tag))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo(recipeId);
                    assertThat(state.getName()).isEqualTo("Pasta carbonara");
                    assertThat(state.getTags()).isEmpty();
                });
    }

    @Test
    void should_not_remove_recipe_tag_when_not_present() {
        String recipeId = "recipeId";

        fixture.given(new RecipeCreatedEvent(recipeId, "Pasta carbonara"))
                .when(new RemoveRecipeTagCommand(recipeId, "recipeTag"))
                .expectException(RecipeTagNotFoundException.class)
                .expectNoEvents();
    }

    @Test
    void should_add_recipe_image() {
        String recipeId = "recipeId";
        String expectedImageId = "imageId";

        fixture.given(new RecipeCreatedEvent(recipeId, "Pasta carbonara"))
                .when(new AddRecipeImageCommand(recipeId, expectedImageId))
                .expectEvents(new RecipeImageAddedEvent(recipeId, expectedImageId))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo(recipeId);
                    assertThat(state.getName()).isEqualTo("Pasta carbonara");
                    assertThat(state.getImages()).containsExactly(expectedImageId);
                });
    }

    @Test
    void should_not_add_recipe_image_if_already_added() {
        String recipeId = "recipeId";
        String imageId = "imageId";

        fixture.given(new RecipeCreatedEvent(recipeId, "Pasta carbonara"),
                        new RecipeImageAddedEvent(recipeId, imageId))
                .when(new AddRecipeImageCommand(recipeId, imageId))
                .expectNoEvents();
    }

    @Test
    void should_remove_recipe_image_when_present() {
        String recipeId = "recipeId";
        String imageId = "imageId";

        fixture.given(new RecipeCreatedEvent(recipeId, "Pasta carbonara"),
                        new RecipeImageAddedEvent(recipeId, imageId))
                .when(new RemoveRecipeImageCommand(recipeId, imageId))
                .expectEvents(new RecipeImageRemovedEvent(recipeId, imageId))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo(recipeId);
                    assertThat(state.getName()).isEqualTo("Pasta carbonara");
                    assertThat(state.getImages()).isEmpty();
                });
    }

    @Test
    void should_not_remove_recipe_image_when_not_present() {
        String recipeId = "recipeId";

        fixture.given(new RecipeCreatedEvent(recipeId, "Pasta carbonara"))
                .when(new RemoveRecipeImageCommand(recipeId, "imageId"))
                .expectException(RecipeImageNotFoundException.class)
                .expectNoEvents();
    }
}
