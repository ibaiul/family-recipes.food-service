package eus.ibai.family.recipes.food.wm.infrastructure.exception;

public class DownstreamConnectivityException extends RuntimeException {

    public DownstreamConnectivityException(String message, Throwable cause) {
        super(message, cause);
    }
}
