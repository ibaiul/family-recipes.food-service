package eus.ibai.family.recipes.food.wm.domain.property;

import eus.ibai.family.recipes.food.event.PropertyCreatedEvent;
import eus.ibai.family.recipes.food.event.PropertyDeletedEvent;
import eus.ibai.family.recipes.food.event.PropertyUpdatedEvent;
import org.axonframework.eventsourcing.AggregateDeletedException;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.aggregate.FixtureConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyAggregateTest {

    private FixtureConfiguration<PropertyAggregate> fixture;

    @BeforeEach
    public void setUp() {
        fixture = new AggregateTestFixture<>(PropertyAggregate.class);
    }

    @Test
    void should_create_property() {
        fixture.givenNoPriorActivity()
                .when(new CreatePropertyCommand("propertyId", "Vitamin C"))
                .expectEvents(new PropertyCreatedEvent("propertyId", "Vitamin C"))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo("propertyId");
                    assertThat(state.getName()).isEqualTo("Vitamin C");
                });
    }

    @Test
    void should_update_property() {
        fixture.given(new PropertyCreatedEvent("propertyId", "Vitamin C"))
                .when(new UpdatePropertyCommand("propertyId", "Vit C"))
                .expectEvents(new PropertyUpdatedEvent("propertyId", "Vit C"))
                .expectState(state -> {
                    assertThat(state.getId()).isEqualTo("propertyId");
                    assertThat(state.getName()).isEqualTo("Vit C");
                });
    }

    @Test
    void should_not_update_property_when_not_changed() {
        fixture.given(new PropertyCreatedEvent("propertyId", "Vitamin C"))
                .when(new UpdatePropertyCommand("propertyId", "Vitamin C"))
                .expectNoEvents();
    }

    @Test
    void should_delete_property() {
        fixture.given(new PropertyCreatedEvent("propertyId", "Vitamin C"))
                .when(new DeletePropertyCommand("propertyId"))
                .expectEvents(new PropertyDeletedEvent("propertyId", "Vitamin C"));
    }

    @Test
    void should_not_accept_commands_when_property_is_deleted() {
        fixture.given(new PropertyCreatedEvent("propertyId", "Vitamin C"),
                        new PropertyDeletedEvent("propertyId", "Vitamin C"))
                .when(new UpdatePropertyCommand("propertyId", "Vit C"))
                .expectException(AggregateDeletedException.class)
                .expectNoEvents();
    }
}
