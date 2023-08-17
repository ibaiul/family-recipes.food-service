package eus.ibai.family.recipes.food.wm.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;
import java.util.regex.Pattern;

public class UrlValidator implements ConstraintValidator<ValidUrl, Set<String>> {

    private static final String URL_REGEX = "^((https?://)(%[0-9A-Fa-f]{2}|[-()_.!~*';/?:@&=+$,A-Za-z0-9])+)$";

    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);

    @Override
    public boolean isValid(Set<String> value, ConstraintValidatorContext context) {
        return value != null && value.stream().allMatch(URL_PATTERN.asMatchPredicate());
    }
}
