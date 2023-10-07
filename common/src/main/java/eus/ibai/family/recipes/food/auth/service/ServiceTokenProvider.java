package eus.ibai.family.recipes.food.auth.service;

import java.util.Optional;

public interface ServiceTokenProvider {

    Optional<String> getServiceToken();
}
