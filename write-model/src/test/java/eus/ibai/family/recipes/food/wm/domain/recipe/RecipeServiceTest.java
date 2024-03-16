package eus.ibai.family.recipes.food.wm.domain.recipe;

import eus.ibai.family.recipes.food.exception.RecipeNotFoundException;
import eus.ibai.family.recipes.food.wm.domain.file.FileStorage;
import eus.ibai.family.recipes.food.wm.domain.file.InvalidFileException;
import eus.ibai.family.recipes.food.wm.domain.ingredient.CreateIngredientCommand;
import eus.ibai.family.recipes.food.wm.infrastructure.exception.DownstreamConnectivityException;
import org.axonframework.eventsourcing.AggregateDeletedException;
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;

import static eus.ibai.family.recipes.food.test.TestUtils.UUID_PATTERN;
import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    private static final RecipeProperties recipeProperties = RecipeProperties.builder()
            .images(new RecipeProperties.ImageProperties("recipes/images/", Set.of("image/png"), 10240, 2048000))
            .build();
    @Captor
    private ArgumentCaptor<CreateRecipeCommand> sentCreateRecipeCommand;

    @Mock
    private ReactorCommandGateway commandGateway;

    @Mock
    private RecipeConstraintRepository recipeConstraintRepository;

    @Mock
    private FileStorage fileStorage;

    private RecipeService recipeService;

    @BeforeEach
    void beforeEach() {
        recipeService = new RecipeServiceImpl(commandGateway, recipeConstraintRepository, fileStorage, recipeProperties);
    }

    @Test
    void should_send_create_recipe_command_when_recipe_name_not_assigned_to_other_recipe() {
        String recipeName = "Pasta carbonara";
        when(recipeConstraintRepository.nameExists(recipeName)).thenReturn(false);
        String expectedRecipeId = generateId();
        when(commandGateway.send(any())).thenReturn(Mono.just(expectedRecipeId));

        recipeService.createRecipe(recipeName)
                .as(StepVerifier::create)
                .expectNext(expectedRecipeId)
                .verifyComplete();

        verify(commandGateway).send(sentCreateRecipeCommand.capture());
        assertThat(sentCreateRecipeCommand.getValue()).matches(command -> recipeName.equals(command.recipeName()));
        assertThat(sentCreateRecipeCommand.getValue()).matches(command -> command.aggregateId() != null);
    }

    @Test
    void should_not_send_create_recipe_command_when_recipe_name_is_assigned_to_other_recipe() {
        when(recipeConstraintRepository.nameExists("Pasta carbonara")).thenReturn(true);

        recipeService.createRecipe("Pasta carbonara")
                .as(StepVerifier::create)
                .verifyError(RecipeAlreadyExistsException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_not_send_create_recipe_command_when_cannot_ensure_if_recipe_name_already_assigned_to_other_recipe() {
        when(recipeConstraintRepository.nameExists("Pasta carbonara")).thenThrow(new RuntimeException(""));

        recipeService.createRecipe("Pasta carbonara")
                .as(StepVerifier::create)
                .verifyError(DownstreamConnectivityException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_send_update_recipe_command_when_recipe_name_not_assigned_to_other_recipe() {
        UpdateRecipeCommand expectedRecipeCommand = new UpdateRecipeCommand(generateId(), "Pasta carbonara", Set.of("https://pasta.com"));
        when(recipeConstraintRepository.anotherRecipeContainsName(expectedRecipeCommand.aggregateId(), "Pasta carbonara")).thenReturn(false);
        when(commandGateway.send(expectedRecipeCommand)).thenReturn(Mono.empty());

        recipeService.updateRecipe(expectedRecipeCommand.aggregateId(), expectedRecipeCommand.recipeName(), Set.of("https://pasta.com"))
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_send_update_recipe_command_when_recipe_name_assigned_to_same_recipe() {
        UpdateRecipeCommand expectedRecipeCommand = new UpdateRecipeCommand(generateId(), "Pasta carbonara", Set.of("https://pasta.com"));
        when(recipeConstraintRepository.anotherRecipeContainsName(expectedRecipeCommand.aggregateId(), "Pasta carbonara")).thenReturn(false);
        when(commandGateway.send(expectedRecipeCommand)).thenReturn(Mono.empty());

        recipeService.updateRecipe(expectedRecipeCommand.aggregateId(), expectedRecipeCommand.recipeName(), Set.of("https://pasta.com"))
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_not_send_update_recipe_command_when_recipe_name_is_assigned_to_other_recipe() {
        when(recipeConstraintRepository.anotherRecipeContainsName("recipeId1", "Pasta carbonara")).thenReturn(true);

        recipeService.updateRecipe("recipeId1", "Pasta carbonara", Set.of("https://pasta.com"))
                .as(StepVerifier::create)
                .verifyError(RecipeAlreadyExistsException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_not_send_update_recipe_command_when_cannot_ensure_if_recipe_name_already_assigned_to_other_recipe() {
        when(recipeConstraintRepository.anotherRecipeContainsName("recipeId", "Pasta carbonara")).thenThrow(new RuntimeException(""));

        recipeService.updateRecipe("recipeId", "Pasta carbonara", Set.of("https://pasta.com"))
                .as(StepVerifier::create)
                .verifyError(DownstreamConnectivityException.class);

        verifyNoInteractions(commandGateway);
    }

    @Test
    void should_fail_to_update_recipe_when_recipe_does_not_exist() {
        when(recipeConstraintRepository.anotherRecipeContainsName("recipeId", "Pasta carbonara")).thenReturn(false);
        when(commandGateway.send(new UpdateRecipeCommand("recipeId", "Pasta carbonara", Set.of("https://pasta.com")))).thenReturn(Mono.error(new AggregateNotFoundException("", "")));

        recipeService.updateRecipe("recipeId", "Pasta carbonara", Set.of("https://pasta.com"))
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_fail_to_update_recipe_when_recipe_already_deleted() {
        when(recipeConstraintRepository.anotherRecipeContainsName("recipeId", "Pasta carbonara")).thenReturn(false);
        when(commandGateway.send(new UpdateRecipeCommand("recipeId", "Pasta carbonara", Set.of("https://pasta.com")))).thenReturn(Mono.error(new AggregateDeletedException("", "message")));

        recipeService.updateRecipe("recipeId", "Pasta carbonara", Set.of("https://pasta.com"))
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_send_delete_recipe_command() {
        DeleteRecipeCommand expectedCommand = new DeleteRecipeCommand("recipeId");
        when(commandGateway.send(expectedCommand)).thenReturn(Mono.empty());

        recipeService.deleteRecipe("recipeId")
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_fail_to_delete_recipe_when_recipe_does_not_exist() {
        DeleteRecipeCommand command = new DeleteRecipeCommand("recipeId");
        when(commandGateway.send(command)).thenReturn(Mono.error(new AggregateNotFoundException("", "")));

        recipeService.deleteRecipe("recipeId")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_fail_to_delete_recipe_when_if_recipe_already_deleted() {
        DeleteRecipeCommand command = new DeleteRecipeCommand("recipeId");
        when(commandGateway.send(command)).thenReturn(Mono.error(new AggregateDeletedException("", "")));

        recipeService.deleteRecipe("recipeId")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_add_existing_ingredient_to_recipe() {
        when(recipeConstraintRepository.retrieveIngredientId("Spaghetti")).thenReturn(Optional.of("ingredientId"));
        when(commandGateway.send(new AddRecipeIngredientCommand("recipeId", "ingredientId"))).thenReturn(Mono.empty());

        recipeService.addRecipeIngredient("recipeId", "Spaghetti")
                .as(StepVerifier::create)
                .expectNext("ingredientId")
                .verifyComplete();
    }

    @Test
    void should_add_non_existing_ingredient_to_recipe() {
        when(recipeConstraintRepository.retrieveIngredientId("Spaghetti")).thenReturn(Optional.empty());
        when(commandGateway.send(any(CreateIngredientCommand.class))).thenReturn(Mono.just(generateId()));
        when(commandGateway.send(any(AddRecipeIngredientCommand.class))).thenReturn(Mono.empty());

        recipeService.addRecipeIngredient("recipeId", "Spaghetti")
                .as(StepVerifier::create)
                .expectNextMatches(recipeId -> UUID_PATTERN.matcher(recipeId).matches())
                .verifyComplete();
    }

    @Test
    void should_not_add_ingredient_to_recipe_if_already_added() {
        when(recipeConstraintRepository.retrieveIngredientId("Spaghetti")).thenReturn(Optional.of("ingredientId"));
        when(commandGateway.send(new AddRecipeIngredientCommand("recipeId", "ingredientId"))).thenReturn(Mono.error(new RecipeIngredientAlreadyAddedException("")));

        recipeService.addRecipeIngredient("recipeId", "Spaghetti")
                .as(StepVerifier::create)
                .verifyError(RecipeIngredientAlreadyAddedException.class);
    }

    @Test
    void should_not_add_ingredient_to_recipe_if_recipe_does_not_exist() {
        when(recipeConstraintRepository.retrieveIngredientId("Spaghetti")).thenReturn(Optional.of("ingredientId"));
        when(commandGateway.send(new AddRecipeIngredientCommand("recipeId", "ingredientId"))).thenReturn(Mono.error(new AggregateNotFoundException("", "")));

        recipeService.addRecipeIngredient("recipeId", "Spaghetti")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_not_add_ingredient_to_recipe_if_recipe_already_deleted() {
        when(recipeConstraintRepository.retrieveIngredientId("Spaghetti")).thenReturn(Optional.of("ingredientId"));
        when(commandGateway.send(new AddRecipeIngredientCommand("recipeId", "ingredientId"))).thenReturn(Mono.error(new AggregateDeletedException("", "")));

        recipeService.addRecipeIngredient("recipeId", "Spaghetti")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_remove_ingredient_from_recipe() {
        when(commandGateway.send(new RemoveRecipeIngredientCommand("recipeId", "ingredientId"))).thenReturn(Mono.empty());

        recipeService.removeRecipeIngredient("recipeId", "ingredientId")
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_not_remove_ingredient_from_recipe_if_not_in_recipe() {
        when(commandGateway.send(new RemoveRecipeIngredientCommand("recipeId", "ingredientId"))).thenReturn(Mono.error(new RecipeIngredientNotFoundException("")));

        recipeService.removeRecipeIngredient("recipeId", "ingredientId")
                .as(StepVerifier::create)
                .verifyError(RecipeIngredientNotFoundException.class);
    }

    @Test
    void should_not_remove_ingredient_from_recipe_if_recipe_does_not_exist() {
        when(commandGateway.send(new RemoveRecipeIngredientCommand("recipeId", "ingredientId"))).thenReturn(Mono.error(new AggregateNotFoundException("", "")));

        recipeService.removeRecipeIngredient("recipeId", "ingredientId")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_not_remove_ingredient_from_recipe_if_recipe_already_deleted() {
        when(commandGateway.send(new RemoveRecipeIngredientCommand("recipeId", "ingredientId"))).thenReturn(Mono.error(new AggregateDeletedException("", "")));

        recipeService.removeRecipeIngredient("recipeId", "ingredientId")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_add_tag_to_recipe() {
        when(commandGateway.send(new AddRecipeTagCommand("recipeId", "tag"))).thenReturn(Mono.empty());

        recipeService.addRecipeTag("recipeId", "tag")
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_not_add_tag_if_recipe_does_not_exist() {
        when(commandGateway.send(new AddRecipeTagCommand("recipeId", "tag"))).thenReturn(Mono.error(new AggregateNotFoundException("", "")));

        recipeService.addRecipeTag("recipeId", "tag")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_not_add_tag_if_recipe_already_deleted() {
        when(commandGateway.send(new AddRecipeTagCommand("recipeId", "tag"))).thenReturn(Mono.error(new AggregateDeletedException("", "")));

        recipeService.addRecipeTag("recipeId", "tag")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_remove_tag_from_recipe() {
        when(commandGateway.send(new RemoveRecipeTagCommand("recipeId", "tag"))).thenReturn(Mono.empty());

        recipeService.removeRecipeTag("recipeId", "tag")
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_not_remove_tag_if_not_in_recipe() {
        when(commandGateway.send(new RemoveRecipeTagCommand("recipeId", "tag"))).thenReturn(Mono.error(new RecipeTagNotFoundException("")));

        recipeService.removeRecipeTag("recipeId", "tag")
                .as(StepVerifier::create)
                .verifyError(RecipeTagNotFoundException.class);
    }

    @Test
    void should_not_remove_tag_if_recipe_does_not_exist() {
        when(commandGateway.send(new RemoveRecipeTagCommand("recipeId", "tag"))).thenReturn(Mono.error(new AggregateNotFoundException("", "")));

        recipeService.removeRecipeTag("recipeId", "tag")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_not_remove_tag_if_recipe_already_deleted() {
        when(commandGateway.send(new RemoveRecipeTagCommand("recipeId", "tag"))).thenReturn(Mono.error(new AggregateDeletedException("", "")));

        recipeService.removeRecipeTag("recipeId", "tag")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_add_image_to_recipe() {
        when(recipeConstraintRepository.idExists("recipeId")).thenReturn(true);
        when(fileStorage.storeFile(eq("recipes/images/"), eq(IMAGE_PNG_VALUE), eq(10240L), any(), any())).thenReturn(Mono.just("imageId"));
        when(commandGateway.send(new AddRecipeImageCommand("recipeId", "imageId"))).thenReturn(Mono.empty());

        recipeService.addRecipeImage("recipeId", IMAGE_PNG_VALUE, 10240L, Flux.just(ByteBuffer.allocate(0)))
                .as(StepVerifier::create)
                .expectNext("imageId")
                .verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 1024})
    void should_not_add_image_when_content_length_too_small(long contentLength) {
        recipeService.addRecipeImage("recipeId", IMAGE_PNG_VALUE, contentLength, Flux.just(ByteBuffer.allocate(0)))
                .as(StepVerifier::create)
                .verifyError(InvalidFileException.class);
    }

    @Test
    void should_not_add_image_when_image_type_not_allowed() {
        recipeService.addRecipeImage("recipeId", IMAGE_GIF_VALUE, 10240L, Flux.just(ByteBuffer.allocate(0)))
                .as(StepVerifier::create)
                .verifyError(InvalidFileException.class);
    }

    @Test
    void should_not_add_nor_upload_image_when_recipe_does_not_exist() {
        when(recipeConstraintRepository.idExists("recipeId")).thenReturn(false);

        recipeService.addRecipeImage("recipeId", IMAGE_PNG_VALUE, 10240L, Flux.just(ByteBuffer.allocate(0)))
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);

        verifyNoMoreInteractions(fileStorage, commandGateway);
    }

    @Test
    void should_not_add_image_to_recipe_when_recipe_already_deleted() {
        when(recipeConstraintRepository.retrieveIngredientId("Spaghetti")).thenReturn(Optional.of("ingredientId"));
        when(commandGateway.send(new AddRecipeIngredientCommand("recipeId", "ingredientId"))).thenReturn(Mono.error(new AggregateDeletedException("", "")));

        recipeService.addRecipeIngredient("recipeId", "Spaghetti")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_remove_image_from_recipe() {
        when(commandGateway.send(new RemoveRecipeImageCommand("recipeId", "imageId"))).thenReturn(Mono.empty());
        when(fileStorage.deleteFile("recipes/images/imageId")).thenReturn(Mono.empty());

        recipeService.removeRecipeImage("recipeId", "imageId")
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_remove_image_from_recipe_and_ignore_errors_when_cannot_delete_image_from_storage() {
        when(commandGateway.send(new RemoveRecipeImageCommand("recipeId", "imageId"))).thenReturn(Mono.empty());
        when(fileStorage.deleteFile("recipes/images/imageId")).thenReturn(Mono.error(new IOException("")));

        recipeService.removeRecipeImage("recipeId", "imageId")
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void should_not_remove_image_if_not_in_recipe() {
        when(commandGateway.send(new RemoveRecipeImageCommand("recipeId", "imageId"))).thenReturn(Mono.error(new RecipeImageNotFoundException("")));

        recipeService.removeRecipeImage("recipeId", "imageId")
                .as(StepVerifier::create)
                .verifyError(RecipeImageNotFoundException.class);
    }

    @Test
    void should_not_remove_image_if_recipe_does_not_exist() {
        when(commandGateway.send(new RemoveRecipeImageCommand("recipeId", "imageId"))).thenReturn(Mono.error(new AggregateNotFoundException("", "")));

        recipeService.removeRecipeImage("recipeId", "imageId")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }

    @Test
    void should_not_remove_image_if_recipe_already_deleted() {
        when(commandGateway.send(new RemoveRecipeImageCommand("recipeId", "imageId"))).thenReturn(Mono.error(new AggregateDeletedException("", "")));

        recipeService.removeRecipeImage("recipeId", "imageId")
                .as(StepVerifier::create)
                .verifyError(RecipeNotFoundException.class);
    }
}
