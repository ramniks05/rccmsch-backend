package in.gov.manipur.rccms.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Deserializes date-only strings (dd-MM-yyyy) to LocalDateTime at 00:00:00.
 */
public class LocalDateTimeDateOnlyDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateStr = p.getValueAsString();
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        LocalDate date = LocalDate.parse(dateStr.trim(), FORMATTER);
        return date.atStartOfDay();
    }
}
