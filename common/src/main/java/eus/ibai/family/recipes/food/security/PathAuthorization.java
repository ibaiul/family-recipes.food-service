package eus.ibai.family.recipes.food.security;

import org.springframework.http.HttpMethod;

public record PathAuthorization(HttpMethod httpMethod, String pathPattern, String[] requiredRoles) {

    /**
     * Constructor for providing a path pattern that does not require any authentication and therefore is open to the public.
     */
    public PathAuthorization(HttpMethod httpMethod, String pathPattern) {
        this(httpMethod, pathPattern, null);
    }
}
