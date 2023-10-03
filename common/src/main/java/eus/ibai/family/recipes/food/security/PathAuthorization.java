package eus.ibai.family.recipes.food.security;

import org.springframework.http.HttpMethod;

public record PathAuthorization(HttpMethod httpMethod, String path, String[] requiredRoles) {

    /**
     * Constructor for providing a path that does not require any authentication and therefore is open to the public.
     */
    public PathAuthorization(HttpMethod httpMethod, String path) {
        this(httpMethod, path, null);
    }
}
