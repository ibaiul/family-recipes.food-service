package eus.ibai.family.recipes.food.wm.domain.command;

import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandExecutionException;
import org.axonframework.messaging.RemoteHandlingException;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RemoteHandlingExceptionMapper implements UnaryOperator<Throwable> {

    private static final Pattern REMOTE_EXCEPTION_MESSAGE_PATTERN = Pattern.compile("(An exception was thrown by the remote message handling component: )([a-zA-Z\\.$]+)(: )(.*)(\nCaused by .*)?");

    @Override
    public Throwable apply(Throwable throwable) {
        if (!(throwable instanceof CommandExecutionException) || !(throwable.getCause() instanceof RemoteHandlingException remoteHandlingException)) {
            return throwable;
        }
        Matcher matcher = REMOTE_EXCEPTION_MESSAGE_PATTERN.matcher(remoteHandlingException.getMessage());
        if (matcher.find()) {
            String originalExceptionClassName = matcher.group(2);
            String originalExceptionMessage = matcher.group(4);

            try {
                Class<Throwable> clazz = (Class<Throwable>) Class.forName(originalExceptionClassName);
                Optional<Constructor<Throwable>> optionalConstructor = Arrays.stream(clazz.getConstructors())
                        .filter(constructor -> Arrays.equals(constructor.getParameterTypes(), new Class[]{String.class}) || Arrays.equals(constructor.getParameterTypes(), new Class[]{String.class, Throwable.class}))
                        .map(constructor -> (Constructor<Throwable>) constructor)
                        .findFirst();
                if (optionalConstructor.isPresent()) {
                    Constructor<Throwable> constructor = optionalConstructor.get();
                    if (constructor.getParameterCount() == 1) {
                        return constructor.newInstance(originalExceptionMessage);
                    } else if (constructor.getParameterCount() == 2) {
                        return constructor.newInstance(originalExceptionMessage, null);
                    }
                }
            } catch (Exception e) {
                log.error("Unable to map RemoteHandlingException with message: {}", remoteHandlingException.getMessage());
                return throwable;
            }
        } else {
            log.error("Unable to map RemoteHandlingException with message: {}", remoteHandlingException.getMessage());
        }

        return throwable;
    }
}
