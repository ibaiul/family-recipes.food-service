package eus.ibai.family.recipes.food.wm.domain.property;

import eus.ibai.family.recipes.food.exception.PropertyNotFoundException;
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

import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Captor
    private ArgumentCaptor<CreatePropertyCommand> sentCreatePropertyCommand;

    @Mock
    private ReactorCommandGateway commandGateway;

    @Mock
    private PropertyConstraintRepository propertyConstraintRepository;

    private PropertyService propertyService;

    @BeforeEach
    void beforeEach() {
        propertyService = new PropertyServiceImpl(commandGateway, propertyConstraintRepository);
    }

    @Test
    void should_send_create_property_command_when_property_name_not_assigned_to_other_property() {
        String propertyName = "Vitamin C";
        when(propertyConstraintRepository.nameExists(propertyName)).thenReturn(false);
        String expectedPropertyId = generateId();
        when(commandGateway.send(any())).thenReturn(Mono.just(expectedPropertyId));

        propertyService.createProperty(propertyName)
                .as(StepVerifier::create)
                .expectNext(expectedPropertyId)
                .verifyComplete();

        verify(commandGateway).send(sentCreatePropertyCommand.capture());
        assertThat(sentCreatePropertyCommand.getValue()).matches(command -> propertyName.equals(command.propertyName()));
        assertThat(sentCreatePropertyCommand.getValue()).matches(command -> command.aggregateId() != null);
    }

    @Test
    void should_not_send_create_property_command_when_property_name_is_assigned_to_other_property() {
        when(propertyConstraintRepository.nameExists("Vitamin C")).thenReturn(true);

        propertyService.createProperty("Vitamin C")
                .as(StepVerifier::create)
                .verifyError(PropertyAlreadyExistsException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_not_send_create_property_command_when_cannot_ensure_if_property_name_already_assigned_to_other_property() {
        when(propertyConstraintRepository.nameExists("Vitamin C")).thenThrow(new RuntimeException(""));

        propertyService.createProperty("Vitamin C")
                .as(StepVerifier::create)
                .verifyError(DownstreamConnectivityException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_send_update_property_command_when_property_name_not_assigned_to_other_property() {
        String propertyName = "Vitamin C";
        UpdatePropertyCommand expectedPropertyCommand = new UpdatePropertyCommand(generateId(), propertyName);
        when(propertyConstraintRepository.anotherPropertyContainsName(expectedPropertyCommand.aggregateId(), propertyName)).thenReturn(false);
        when(commandGateway.send(expectedPropertyCommand)).thenReturn(Mono.empty());

        propertyService.updateProperty(expectedPropertyCommand.aggregateId(), expectedPropertyCommand.propertyName())
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_send_update_property_command_when_property_name_assigned_to_same_property() {
        UpdatePropertyCommand command = new UpdatePropertyCommand(generateId(), "Vitamin C");
        when(propertyConstraintRepository.anotherPropertyContainsName(command.aggregateId(), command.propertyName())).thenReturn(false);
        when(commandGateway.send(command)).thenReturn(Mono.empty());

        propertyService.updateProperty(command.aggregateId(), command.propertyName())
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_not_send_update_property_command_when_property_name_is_assigned_to_other_property() {
        when(propertyConstraintRepository.anotherPropertyContainsName("propertyId", "Vitamin C")).thenReturn(true);

        propertyService.updateProperty("propertyId", "Vitamin C")
                .as(StepVerifier::create)
                .verifyError(PropertyAlreadyExistsException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_not_send_update_property_command_when_cannot_ensure_if_property_name_already_assigned_to_other_property() {
        when(propertyConstraintRepository.anotherPropertyContainsName("propertyId", "Vitamin C")).thenThrow(new RuntimeException(""));

        propertyService.updateProperty("propertyId", "Vitamin C")
                .as(StepVerifier::create)
                .verifyError(DownstreamConnectivityException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_fail_to_update_property_when_property_does_not_exist() {
        when(propertyConstraintRepository.anotherPropertyContainsName("propertyId", "Vitamin C")).thenReturn(false);
        when(commandGateway.send(new UpdatePropertyCommand("propertyId", "Vitamin C"))).thenReturn(Mono.error(new AggregateNotFoundException("propertyId", "message")));

        propertyService.updateProperty("propertyId", "Vitamin C")
                .as(StepVerifier::create)
                .verifyError(PropertyNotFoundException.class);
    }

    @Test
    void should_fail_to_update_property_when_property_already_deleted() {
        when(propertyConstraintRepository.anotherPropertyContainsName("propertyId", "Vitamin C")).thenReturn(false);
        when(commandGateway.send(new UpdatePropertyCommand("propertyId", "Vitamin C"))).thenReturn(Mono.error(new AggregateDeletedException("propertyId", "message")));

        propertyService.updateProperty("propertyId", "Vitamin C")
                .as(StepVerifier::create)
                .verifyError(PropertyNotFoundException.class);
    }

    @Test
    void should_send_delete_property_command_when_property_is_not_bound_to_any_ingredient() {
        when(propertyConstraintRepository.isPropertyBoundToIngredients("propertyId")).thenReturn(false);
        DeletePropertyCommand expectedCommand = new DeletePropertyCommand("propertyId");
        when(commandGateway.send(expectedCommand)).thenReturn(Mono.empty());

        propertyService.deleteProperty("propertyId")
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_not_send_delete_property_command_when_property_is_bound_to_any_ingredient() {
        when(propertyConstraintRepository.isPropertyBoundToIngredients("propertyId")).thenReturn(true);

        propertyService.deleteProperty("propertyId")
                .as(StepVerifier::create)
                .verifyError(PropertyAttachedToIngredientException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_not_send_delete_property_command_when_cannot_ensure_if_property_is_bound_to_any_ingredient() {
        when(propertyConstraintRepository.isPropertyBoundToIngredients("propertyId")).thenThrow(new RuntimeException(""));

        propertyService.deleteProperty("propertyId")
                .as(StepVerifier::create)
                .verifyError(DownstreamConnectivityException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_fail_to_delete_property_when_property_does_not_exist() {
        when(propertyConstraintRepository.isPropertyBoundToIngredients("propertyId")).thenReturn(false);
        DeletePropertyCommand command = new DeletePropertyCommand("propertyId");
        when(commandGateway.send(command)).thenReturn(Mono.error(new AggregateNotFoundException("propertyId", "message")));

        propertyService.deleteProperty("propertyId")
                .as(StepVerifier::create)
                .verifyError(PropertyNotFoundException.class);
    }

    @Test
    void should_fail_to_delete_property_when_if_property_already_deleted() {
        when(propertyConstraintRepository.isPropertyBoundToIngredients("propertyId")).thenReturn(false);
        DeletePropertyCommand command = new DeletePropertyCommand("propertyId");
        when(commandGateway.send(command)).thenReturn(Mono.error(new AggregateDeletedException("propertyId", "message")));

        propertyService.deleteProperty("propertyId")
                .as(StepVerifier::create)
                .verifyError(PropertyNotFoundException.class);
    }
}
