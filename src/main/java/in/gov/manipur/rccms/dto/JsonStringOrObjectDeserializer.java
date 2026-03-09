package in.gov.manipur.rccms.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Deserializes a field that may be sent as either a JSON string or a JSON object.
 * If the client sends an object (e.g. {"khasra_no":"1//2","owner_name":"..."}),
 * it is converted to a JSON string so the field (caseData, formData) is always stored as String.
 * This avoids "Cannot deserialize value of type java.lang.String from Object value" when
 * the frontend sends caseData/formData as an object instead of a stringified JSON.
 */
public class JsonStringOrObjectDeserializer extends JsonDeserializer<String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isObject() || node.isArray()) {
            return MAPPER.writeValueAsString(node);
        }
        return node.asText();
    }
}
