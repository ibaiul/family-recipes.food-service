package eus.ibai.family.recipes.food.wm.domain.ingredient;

import eus.ibai.family.recipes.food.event.*;
import eus.ibai.family.recipes.food.test.TestUtils;
import org.axonframework.eventsourcing.AggregateDeletedException;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static eus.ibai.family.recipes.food.test.TestUtils.fixedTime;
import static org.assertj.core.api.Assertions.assertThat;

class IngredientAggregateTest {

    private FixtureConfiguration<IngredientAggregate> fixture;

    @BeforeEach
    public void setUp() {
        fixture = new AggregateTestFixture<>(IngredientAggregate.class);
        fixture.registerInjectableResource(TestUtils.FIXED_CLOCK);
    }

    @Test
    void should_create_ingredient() {
        fixture.givenNoPriorActivity()
                .when(new CreateIngredientCommand("ingredientId", "Spaghetti"))
                .expectEvents(new IngredientCreatedEvent("ingredientId", "Spaghetti"))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo("ingredientId");
                    assertThat(state.getName()).isEqualTo("Spaghetti");
                    assertThat(state.getProperties()).isEmpty();
                });
    }

    @Test
    void should_update_ingredient() {
        fixture.given(new IngredientCreatedEvent("ingredientId", "Spaghetti"))
                .when(new UpdateIngredientCommand("ingredientId", "Spaghetti integral"))
                .expectEvents(new IngredientUpdatedEvent("ingredientId", "Spaghetti integral"))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo("ingredientId");
                    assertThat(state.getName()).isEqualTo("Spaghetti integral");
                    assertThat(state.getProperties()).isEmpty();
                });
    }

    @Test
    void should_not_update_ingredient_when_not_changed() {
        fixture.given(new IngredientCreatedEvent("ingredientId", "Spaghetti"))
                .when(new UpdateIngredientCommand("ingredientId", "Spaghetti"))
                .expectNoEvents();
    }

    @Test
    void should_delete_ingredient() {
        fixture.given(new IngredientCreatedEvent("ingredientId", "Spaghetti"))
                .when(new DeleteIngredientCommand("ingredientId"))
                .expectEvents(new IngredientDeletedEvent("ingredientId", "Spaghetti"));
    }

    @Test
    void should_not_accept_commands_when_ingredient_is_deleted() {
        fixture.given(new IngredientCreatedEvent("ingredientId", "Spaghetti"),
                        new IngredientDeletedEvent("ingredientId", "Spaghetti"))
                .when(new UpdateIngredientCommand("ingredientId", "Spaghetti integral"))
                .expectException(AggregateDeletedException.class)
                .expectNoEvents();
    }

    @Test
    void should_add_ingredient_property() {
        IngredientProperty ingredientProperty = new IngredientProperty("propertyId", fixedTime());
        IngredientPropertyEntity expectedIngredientProperty = new IngredientPropertyEntity(ingredientProperty.propertyId(), ingredientProperty.addedOn());

        fixture.given(new IngredientCreatedEvent("ingredientId", "Spaghetti"))
                .when(new AddIngredientPropertyCommand("ingredientId", "propertyId"))
                .expectEvents(new IngredientPropertyAddedEvent("ingredientId", ingredientProperty))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo("ingredientId");
                    assertThat(state.getName()).isEqualTo("Spaghetti");
                    assertThat(state.getProperties()).containsExactly(expectedIngredientProperty);
                });
    }

    @Test
    void should_not_add_ingredient_property_if_already_added() {
        IngredientProperty ingredientProperty = new IngredientProperty("propertyId", fixedTime());

        fixture.given(new IngredientCreatedEvent("ingredientId", "Spaghetti"),
                        new IngredientPropertyAddedEvent("ingredientId", ingredientProperty))
                .when(new AddIngredientPropertyCommand("ingredientId", "propertyId"))
                .expectException(IngredientPropertyAlreadyAddedException.class)
                .expectNoEvents();
    }

    @Test
    void should_remove_ingredient_property_when_present() {
        IngredientProperty ingredientProperty = new IngredientProperty("propertyId", fixedTime());

        fixture.given(new IngredientCreatedEvent("ingredientId", "Spaghetti"),
                        new IngredientPropertyAddedEvent("ingredientId", ingredientProperty))
                .when(new RemoveIngredientPropertyCommand("ingredientId", "propertyId"))
                .expectEvents(new IngredientPropertyRemovedEvent("ingredientId", ingredientProperty))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo("ingredientId");
                    assertThat(state.getName()).isEqualTo("Spaghetti");
                    assertThat(state.getProperties()).isEmpty();
                });
    }

    @Test
    void should_not_remove_ingredient_property_when_not_present() {
        fixture.given(new IngredientCreatedEvent("ingredientId", "Spaghetti"))
                .when(new RemoveIngredientPropertyCommand("ingredientId", "propertyId"))
                .expectException(IngredientPropertyNotFoundException.class)
                .expectNoEvents();
    }
}
