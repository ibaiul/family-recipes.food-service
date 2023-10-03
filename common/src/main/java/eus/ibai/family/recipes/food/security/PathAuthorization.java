package eus.ibai.family.recipes.food.security;

import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.Objects;

public record PathAuthorization(HttpMethod httpMethod, String pathPattern, String[] requiredRoles) {

    /**
     * Constructor for providing a path pattern that does not require any authentication and therefore is open to the public.
     */
    public PathAuthorization(HttpMethod httpMethod, String path) {
        this(httpMethod, path, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PathAuthorization that = (PathAuthorization) o;
        return Objects.equals(httpMethod, that.httpMethod) && Objects.equals(pathPattern, that.pathPattern) && Arrays.equals(requiredRoles, that.requiredRoles);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(httpMethod, pathPattern);
        result = 31 * result + Arrays.hashCode(requiredRoles);
        return result;
    }

    @Override
    public String toString() {
        return "PathAuthorization{" +
                "httpMethod=" + httpMethod +
                ", pathPattern='" + pathPattern + '\'' +
                ", requiredRoles=" + Arrays.toString(requiredRoles) +
                '}';
    }
}
