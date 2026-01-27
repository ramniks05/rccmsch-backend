package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual condition status in checklist
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionStatusDTO {
    
    private String label;
    private String type; // WORKFLOW_FLAG, FORM_FIELD, CASE_DATA_FIELD, CASE_FILTER
    private String flagName; // For WORKFLOW_FLAG type
    private String moduleType; // For FORM_FIELD type
    private String fieldName; // For FORM_FIELD or CASE_DATA_FIELD type
    private Boolean required;
    private Boolean passed;
    private String message; // User-friendly message explaining the condition
}
