package eus.ibai.family.recipes.food.rm.test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import eus.ibai.family.recipes.food.event.DomainEvent;
import eus.ibai.family.recipes.food.event.IngredientCreatedEvent;
import eus.ibai.family.recipes.food.event.RecipeCreatedEvent;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

@JsonComponent
public class DomainEventDeserializer extends StdDeserializer<DomainEvent<String>> {

    public DomainEventDeserializer() {
        this(null);
    }

    public DomainEventDeserializer(Class<DomainEvent> vc) {
        super(vc);
    }

    @Override
    public DomainEvent<String> deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode jsonNode = mapper.readTree(jsonParser);

        String eventType = jsonNode.get("type").asText();
        return switch (eventType) {
            case "RecipeCreatedEvent" -> mapper.treeToValue(jsonNode, RecipeCreatedEvent.class);
            case "IngredientCreatedEvent" -> mapper.treeToValue(jsonNode, IngredientCreatedEvent.class);
            default -> throw new IllegalStateException("Unexpected domain event type : " + eventType);
        };
    }
}
