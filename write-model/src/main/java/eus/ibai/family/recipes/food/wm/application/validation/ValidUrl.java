package eus.ibai.family.recipes.food.wm.application.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UrlValidator.class)
public @interface ValidUrl {

    String message() default "Set cannot contain invalid URLs";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
