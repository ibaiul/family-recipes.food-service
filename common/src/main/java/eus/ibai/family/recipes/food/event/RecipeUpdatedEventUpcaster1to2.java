package eus.ibai.family.recipes.food.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.axonframework.serialization.SimpleSerializedType;
import org.axonframework.serialization.upcasting.event.IntermediateEventRepresentation;
import org.axonframework.serialization.upcasting.event.SingleEventUpcaster;

public class RecipeUpdatedEventUpcaster1to2 extends SingleEventUpcaster {

    private static final SimpleSerializedType TARGET_TYPE = new SimpleSerializedType(RecipeUpdatedEvent.class.getTypeName(), null);

    @Override
    protected boolean canUpcast(IntermediateEventRepresentation intermediateRepresentation) {
        return intermediateRepresentation.getType().equals(TARGET_TYPE);
    }

    @Override
    protected IntermediateEventRepresentation doUpcast(IntermediateEventRepresentation intermediateRepresentation) {
        return intermediateRepresentation.upcastPayload(new SimpleSerializedType(TARGET_TYPE.getName(), "2.0"), JsonNode.class, event -> {
            ((ObjectNode) event).putArray("tags");
            return event;
        });
    }
}