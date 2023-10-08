package eus.ibai.family.recipes.food.util;

import java.util.UUID;

public class Utils {

    private Utils() {}

    public static String generateId() {
        return UUID.randomUUID().toString();
    }
}
