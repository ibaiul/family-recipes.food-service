package eus.ibai.family.recipes.food.rm.domain.property;

import eus.ibai.family.recipes.food.exception.PropertyNotFoundException;
import eus.ibai.family.recipes.food.rm.infrastructure.model.PropertyEntity;
import eus.ibai.family.recipes.food.rm.infrastructure.repository.PropertyEntityRepository;
import eus.ibai.family.recipes.food.rm.test.DataCleanupExtension;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static eus.ibai.family.recipes.food.util.Utils.generateId;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(DataCleanupExtension.class)
class PropertyProjectionQueryIT {

    @Autowired
    private ReactorQueryGateway queryGateway;

    @Autowired
    private PropertyEntityRepository propertyEntityRepository;

    private List<PropertyProjection> existingProperties;

    @BeforeEach
    void beforeEach() {
        existingProperties = createProperties();
    }

    @Test
    void should_find_all_properties() {
        queryGateway.streamingQuery(new FindAllPropertiesQuery(), PropertyProjection.class)
                .as(StepVerifier::create)
                .expectNext(existingProperties.toArray(new PropertyProjection[0]))
                .verifyComplete();
    }

    @Test
    void should_find_property_by_id() {
        PropertyProjection expectedProperty = existingProperties.get(0);
        queryGateway.query(new FindPropertyByIdQuery(expectedProperty.id()), PropertyProjection.class)
                .as(StepVerifier::create)
                .expectNext(expectedProperty)
                .verifyComplete();
    }

    @Test
    void should_not_find_property_by_id_if_does_not_exist() {
        queryGateway.query(new FindPropertyByIdQuery(generateId()), PropertyProjection.class)
                .as(StepVerifier::create)
                .verifyError(PropertyNotFoundException.class);
    }

    private List<PropertyProjection> createProperties() {
        return Flux.fromIterable(List.of(
                        new PropertyEntity(generateId(), "Calcium"),
                        new PropertyEntity(generateId(), "Carbohydrates")))
                .flatMap(propertyEntity -> propertyEntityRepository.saveNew(propertyEntity.getId(), propertyEntity.getName())
                        .thenReturn(new PropertyProjection(propertyEntity.getId(), propertyEntity.getName())))
                .collectList().block();
    }
}
