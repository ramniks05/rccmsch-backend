package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO combining module form schema and form data
 * Used to return both schema (for building form) and data (for populating form)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleFormWithDataDTO {
    private ModuleFormSchemaDTO schema;
    private Map<String, Object> formData; // Parsed form data, null if no submission exists
    private Boolean hasExistingData; // Whether form data exists
}
