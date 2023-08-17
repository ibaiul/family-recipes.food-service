package eus.ibai.family.recipes.food.rm.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eus.ibai.family.recipes.food.event.DomainEvent;
import lombok.SneakyThrows;

public class TestUtils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(DomainEvent.class, new DomainEventDeserializer());
        OBJECT_MAPPER.registerModule(simpleModule);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @SneakyThrows
    public static DomainEvent<String> deserialize(String event) {
        TypeReference<DomainEvent<String>> typeReference = new TypeReference<>() {};
        return OBJECT_MAPPER.readValue(event, typeReference);
    }
}
