package eus.ibai.family.recipes.food.wm.domain.command;

import eus.ibai.family.recipes.food.wm.domain.recipe.RecipeIngredientAlreadyAddedException;
import eus.ibai.family.recipes.food.wm.infrastructure.exception.DownstreamConnectivityException;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.messaging.RemoteExceptionDescription;
import org.axonframework.messaging.RemoteHandlingException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteHandlingExceptionMapperTest {

    private final RemoteHandlingExceptionMapper exceptionMapper = new RemoteHandlingExceptionMapper();

    @ParameterizedTest
    @MethodSource
    void should_map_to_original_exception(Throwable remoteException, Class<Throwable> originalExceptionClass) {
        assertThat(exceptionMapper.apply(remoteException)).isExactlyInstanceOf(originalExceptionClass);
    }

    private static Stream<Arguments> should_map_to_original_exception() {
        DownstreamConnectivityException connectivityException = new DownstreamConnectivityException("Root message", new RuntimeException("Child message"));
        RemoteHandlingException remoteHandlingException1 = new RemoteHandlingException(RemoteExceptionDescription.describing(connectivityException));
        RecipeIngredientAlreadyAddedException alreadyAddedException = new RecipeIngredientAlreadyAddedException("Root message");
        RemoteHandlingException remoteHandlingException2 = new RemoteHandlingException(RemoteExceptionDescription.describing(alreadyAddedException));
        return Stream.of(
                Arguments.of(new CommandExecutionException("", remoteHandlingException1), DownstreamConnectivityException.class),
                Arguments.of(new CommandExecutionException("", remoteHandlingException2), RecipeIngredientAlreadyAddedException.class)
        );
    }

    @ParameterizedTest
    @MethodSource
    void should_not_map_to_original_exception_when_not_valid_input(Throwable remoteException, Class<Throwable> expectedExceptionClass) {
        assertThat(exceptionMapper.apply(remoteException)).isExactlyInstanceOf(expectedExceptionClass);
    }

    private static Stream<Arguments> should_not_map_to_original_exception_when_not_valid_input() {
        RecipeIngredientAlreadyAddedException alreadyAddedException = new RecipeIngredientAlreadyAddedException("Root message");
        RemoteHandlingException malformedMessageException = new RemoteHandlingException(new RemoteExceptionDescription(List.of("eus.ibai.Non-Matching-Pattern: Root message")));
        RemoteHandlingException nonExistingException = new RemoteHandlingException(new RemoteExceptionDescription(List.of("eus.ibai.NonExistingException: Root message")));
        RemoteHandlingException nonMatchingConstructorException = new RemoteHandlingException(RemoteExceptionDescription.describing(new NoArgConstructorException()));
        RemoteHandlingException validRemoteHandlingException = new RemoteHandlingException(RemoteExceptionDescription.describing(alreadyAddedException));
        return Stream.of(
                Arguments.of(new CommandExecutionException("", malformedMessageException), CommandExecutionException.class),
                Arguments.of(new CommandExecutionException("", nonExistingException), CommandExecutionException.class),
                Arguments.of(new CommandExecutionException("", nonMatchingConstructorException), CommandExecutionException.class),
                Arguments.of(new IllegalArgumentException("", validRemoteHandlingException), IllegalArgumentException.class),
                Arguments.of(validRemoteHandlingException, RemoteHandlingException.class)
        );
    }

    private static class NoArgConstructorException extends Exception {}
}