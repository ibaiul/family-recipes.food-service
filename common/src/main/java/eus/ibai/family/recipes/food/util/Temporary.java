package eus.ibai.family.recipes.food.util;

import java.lang.annotation.*;


/**
 * Temporary code to allow releasing the service with certain functionality.
 * When I find some time it will be removed and re-implemented in a different way, maybe even in a dedicated microservice.
 */

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface Temporary {

    String value();
}
