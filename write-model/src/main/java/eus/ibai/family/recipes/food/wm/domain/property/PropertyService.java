package eus.ibai.family.recipes.food.wm.domain.property;

import reactor.core.publisher.Mono;

public interface PropertyService {

    Mono<String> createProperty(String propertyName);

    Mono<Void> updateProperty(String propertyId, String propertyName);

    Mono<Void> deleteProperty(String propertyId);
}
