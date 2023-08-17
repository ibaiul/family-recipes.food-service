package eus.ibai.family.recipes.food.util;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class Utils {

    private Utils() {}

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static String maskUsername(@NotNull String username) {
        char[] maskedUsername = new char[username.length()];
        for (int i = 0; i < username.length(); i++) {
            maskedUsername[i] = i % 2 == 0 ?  '*' : username.charAt(i);
        }
        return new String(maskedUsername);
    }
}
