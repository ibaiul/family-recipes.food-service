package eus.ibai.family.recipes.food.security;

public class InvalidJwtTokenException extends Exception {

    public InvalidJwtTokenException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public InvalidJwtTokenException(String message) {
        super(message);
    }
}
