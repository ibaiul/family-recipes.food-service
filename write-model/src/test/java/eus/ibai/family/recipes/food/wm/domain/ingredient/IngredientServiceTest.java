package eus.ibai.family.recipes.food.wm.domain.ingredient;

import eus.ibai.family.recipes.food.exception.IngredientNotFoundException;
import eus.ibai.family.recipes.food.wm.domain.property.CreatePropertyCommand;
import eus.ibai.family.recipes.food.wm.infrastructure.exception.DownstreamConnectivityException;
import org.axonframework.eventsourcing.AggregateDeletedException;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static eus.ibai.family.recipes.food.test.TestUtils.UUID_PATTERN;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Captor
    private ArgumentCaptor<CreateIngredientCommand> sentCreateIngredientCommand;

    @Mock
    private ReactorCommandGateway commandGateway;

    @Mock
    private IngredientConstraintRepository ingredientConstraintRepository;

    private IngredientService ingredientService;

    @BeforeEach
    void beforeEach() {
        ingredientService = new IngredientServiceImpl(commandGateway, ingredientConstraintRepository);
    }

    @Test
    void should_send_create_ingredient_command_when_ingredient_name_not_assigned_to_other_ingredient() {
        String ingredientName = "Spaghetti";
        when(ingredientConstraintRepository.nameExists(ingredientName)).thenReturn(false);
        String expectedIngredientId = generateId();
        when(commandGateway.send(any())).thenReturn(Mono.just(expectedIngredientId));

        ingredientService.createIngredient(ingredientName)
                .as(StepVerifier::create)
                .expectNext(expectedIngredientId)
                .verifyComplete();

        verify(commandGateway).send(sentCreateIngredientCommand.capture());
        assertThat(sentCreateIngredientCommand.getValue()).matches(command -> ingredientName.equals(command.ingredientName()));
        assertThat(sentCreateIngredientCommand.getValue()).matches(command -> command.aggregateId() != null);
    }

    @Test
    void should_not_send_create_ingredient_command_when_ingredient_name_is_assigned_to_other_ingredient() {
        when(ingredientConstraintRepository.nameExists("Spaghetti")).thenReturn(true);

        ingredientService.createIngredient("Spaghetti")
                .as(StepVerifier::create)
                .verifyError(IngredientAlreadyExistsException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_not_send_create_ingredient_command_when_cannot_ensure_if_ingredient_name_already_assigned_to_other_ingredient() {
        when(ingredientConstraintRepository.nameExists("Spaghetti")).thenThrow(new RuntimeException(""));

        ingredientService.createIngredient("Spaghetti")
                .as(StepVerifier::create)
                .verifyError(DownstreamConnectivityException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_send_update_ingredient_command_when_ingredient_name_not_assigned_to_other_ingredient() {
        UpdateIngredientCommand expectedIngredientCommand = new UpdateIngredientCommand(generateId(), "Spaghetti");
        when(ingredientConstraintRepository.anotherIngredientContainsName(expectedIngredientCommand.aggregateId(), "Spaghetti")).thenReturn(false);
        when(commandGateway.send(expectedIngredientCommand)).thenReturn(Mono.empty());

        ingredientService.updateIngredient(expectedIngredientCommand.aggregateId(), expectedIngredientCommand.ingredientName())
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_send_update_ingredient_command_when_ingredient_name_assigned_to_same_ingredient() {
        UpdateIngredientCommand expectedIngredientCommand = new UpdateIngredientCommand(generateId(), "Spaghetti");
        when(ingredientConstraintRepository.anotherIngredientContainsName(expectedIngredientCommand.aggregateId(), "Spaghetti")).thenReturn(false);
        when(commandGateway.send(expectedIngredientCommand)).thenReturn(Mono.empty());

        ingredientService.updateIngredient(expectedIngredientCommand.aggregateId(), expectedIngredientCommand.ingredientName())
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_not_send_update_ingredient_command_when_ingredient_name_is_assigned_to_other_ingredient() {
        when(ingredientConstraintRepository.anotherIngredientContainsName("ingredientId", "Spaghetti")).thenReturn(true);

        ingredientService.updateIngredient("ingredientId", "Spaghetti")
                .as(StepVerifier::create)
                .verifyError(IngredientAlreadyExistsException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_not_send_update_ingredient_command_when_cannot_ensure_if_ingredient_name_already_assigned_to_other_ingredient() {
        when(ingredientConstraintRepository.anotherIngredientContainsName("ingredientId", "Spaghetti")).thenThrow(new RuntimeException(""));

        ingredientService.updateIngredient("ingredientId", "Spaghetti")
                .as(StepVerifier::create)
                .verifyError(DownstreamConnectivityException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_fail_to_update_ingredient_when_ingredient_does_not_exist() {
        when(ingredientConstraintRepository.anotherIngredientContainsName("ingredientId", "Spaghetti")).thenReturn(false);
        when(commandGateway.send(new UpdateIngredientCommand("ingredientId", "Spaghetti"))).thenReturn(Mono.error(new AggregateNotFoundException("", "")));

        ingredientService.updateIngredient("ingredientId", "Spaghetti")
                .as(StepVerifier::create)
                .verifyError(IngredientNotFoundException.class);
    }

    @Test
    void should_fail_to_update_ingredient_when_ingredient_already_deleted() {
        when(ingredientConstraintRepository.anotherIngredientContainsName("ingredientId", "Spaghetti")).thenReturn(false);
        when(commandGateway.send(new UpdateIngredientCommand("ingredientId", "Spaghetti"))).thenReturn(Mono.error(new AggregateDeletedException("", "message")));

        ingredientService.updateIngredient("ingredientId", "Spaghetti")
                .as(StepVerifier::create)
                .verifyError(IngredientNotFoundException.class);
    }

    @Test
    void should_send_delete_ingredient_command_when_ingredient_is_not_bound_to_any_recipe() {
        when(ingredientConstraintRepository.isIngredientBoundToRecipes("ingredientId")).thenReturn(false);
        DeleteIngredientCommand expectedCommand = new DeleteIngredientCommand("ingredientId");
        when(commandGateway.send(expectedCommand)).thenReturn(Mono.empty());

        ingredientService.deleteIngredient("ingredientId")
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_not_send_delete_ingredient_command_when_ingredient_is_bound_to_any_ingredient() {
        when(ingredientConstraintRepository.isIngredientBoundToRecipes("ingredientId")).thenReturn(true);

        ingredientService.deleteIngredient("ingredientId")
                .as(StepVerifier::create)
                .verifyError(IngredientAttachedToRecipeException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_not_send_delete_ingredient_command_when_cannot_ensure_if_ingredient_is_bound_to_any_ingredient() {
        when(ingredientConstraintRepository.isIngredientBoundToRecipes("ingredientId")).thenThrow(new RuntimeException(""));

        ingredientService.deleteIngredient("ingredientId")
                .as(StepVerifier::create)
                .verifyError(DownstreamConnectivityException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_fail_to_delete_ingredient_when_ingredient_does_not_exist() {
        when(ingredientConstraintRepository.isIngredientBoundToRecipes("ingredientId")).thenReturn(false);
        DeleteIngredientCommand command = new DeleteIngredientCommand("ingredientId");
        when(commandGateway.send(command)).thenReturn(Mono.error(new AggregateNotFoundException("", "")));

        ingredientService.deleteIngredient("ingredientId")
                .as(StepVerifier::create)
                .verifyError(IngredientNotFoundException.class);
    }

    @Test
    void should_fail_to_delete_ingredient_when_if_ingredient_already_deleted() {
        when(ingredientConstraintRepository.isIngredientBoundToRecipes("ingredientId")).thenReturn(false);
        DeleteIngredientCommand command = new DeleteIngredientCommand("ingredientId");
        when(commandGateway.send(command)).thenReturn(Mono.error(new AggregateDeletedException("", "")));

        ingredientService.deleteIngredient("ingredientId")
                .as(StepVerifier::create)
                .verifyError(IngredientNotFoundException.class);
    }

    @Test
    void should_add_existing_property_to_ingredient() {
        when(ingredientConstraintRepository.retrievePropertyId("Vitamin C")).thenReturn(Optional.of("propertyId"));
        when(commandGateway.send(new AddIngredientPropertyCommand("ingredientId", "propertyId"))).thenReturn(Mono.empty());

        ingredientService.addIngredientProperty("ingredientId", "Vitamin C")
                .as(StepVerifier::create)
                .expectNext("propertyId")
                .verifyComplete();
    }

    @Test
    void should_add_non_existing_property_to_ingredient() {
        when(ingredientConstraintRepository.retrievePropertyId("Vitamin C")).thenReturn(Optional.empty());
        when(commandGateway.send(any(CreatePropertyCommand.class))).thenReturn(Mono.just(generateId()));
        when(commandGateway.send(any(AddIngredientPropertyCommand.class))).thenReturn(Mono.empty());

        ingredientService.addIngredientProperty("ingredientId", "Vitamin C")
                .as(StepVerifier::create)
                .expectNextMatches(ingredientId -> UUID_PATTERN.matcher(ingredientId).matches())
                .verifyComplete();
    }

    @Test
    void should_not_add_property_to_ingredient_if_already_added() {
        when(ingredientConstraintRepository.retrievePropertyId("Vitamin C")).thenReturn(Optional.of("propertyId"));
        when(commandGateway.send(new AddIngredientPropertyCommand("ingredientId", "propertyId"))).thenReturn(Mono.error(new IngredientPropertyAlreadyAddedException("")));

        ingredientService.addIngredientProperty("ingredientId", "Vitamin C")
                .as(StepVerifier::create)
                .verifyError(IngredientPropertyAlreadyAddedException.class);
    }

    @Test
    void should_not_add_property_to_ingredient_if_ingredient_does_not_exist() {
        when(ingredientConstraintRepository.retrievePropertyId("Vitamin C")).thenReturn(Optional.of("propertyId"));
        when(commandGateway.send(new AddIngredientPropertyCommand("ingredientId", "propertyId"))).thenReturn(Mono.error(new AggregateNotFoundException("", "")));

        ingredientService.addIngredientProperty("ingredientId", "Vitamin C")
                .as(StepVerifier::create)
                .verifyError(IngredientNotFoundException.class);
    }

    @Test
    void should_not_add_property_to_ingredient_if_ingredient_already_deleted() {
        when(ingredientConstraintRepository.retrievePropertyId("Vitamin C")).thenReturn(Optional.of("propertyId"));
        when(commandGateway.send(new AddIngredientPropertyCommand("ingredientId", "propertyId"))).thenReturn(Mono.error(new AggregateDeletedException("", "")));

        ingredientService.addIngredientProperty("ingredientId", "Vitamin C")
                .as(StepVerifier::create)
                .verifyError(IngredientNotFoundException.class);
    }

    @Test
    void should_remove_property_from_ingredient() {
        when(commandGateway.send(new RemoveIngredientPropertyCommand("ingredientId", "propertyId"))).thenReturn(Mono.empty());

        ingredientService.removeIngredientProperty("ingredientId", "propertyId")
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_not_remove_property_from_ingredient_if_not_in_ingredient() {
        when(commandGateway.send(new RemoveIngredientPropertyCommand("ingredientId", "propertyId"))).thenReturn(Mono.error(new IngredientPropertyNotFoundException("")));

        ingredientService.removeIngredientProperty("ingredientId", "propertyId")
                .as(StepVerifier::create)
                .verifyError(IngredientPropertyNotFoundException.class);
    }

    @Test
    void should_not_remove_property_from_ingredient_if_ingredient_does_not_exist() {
        when(commandGateway.send(new RemoveIngredientPropertyCommand("ingredientId", "propertyId"))).thenReturn(Mono.error(new AggregateNotFoundException("", "")));

        ingredientService.removeIngredientProperty("ingredientId", "propertyId")
                .as(StepVerifier::create)
                .verifyError(IngredientNotFoundException.class);
    }
    @Test
    void should_not_remove_property_from_ingredient_if_ingredient_already_deleted() {
        when(commandGateway.send(new RemoveIngredientPropertyCommand("ingredientId", "propertyId"))).thenReturn(Mono.error(new AggregateDeletedException("", "")));

        ingredientService.removeIngredientProperty("ingredientId", "propertyId")
                .as(StepVerifier::create)
                .verifyError(IngredientNotFoundException.class);
    }

}
