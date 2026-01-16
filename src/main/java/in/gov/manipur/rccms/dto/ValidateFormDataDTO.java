package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for validating form data against schema
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateFormDataDTO {
    
    @NotNull(message = "Case type ID is required")
    private Long caseTypeId;
    
    @NotNull(message = "Form data is required")
    private Map<String, Object> formData; // Key-value pairs of fieldName -> value
}

