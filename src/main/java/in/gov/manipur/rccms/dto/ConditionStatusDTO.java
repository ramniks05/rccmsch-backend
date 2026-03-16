package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for individual condition status in checklist
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionStatusDTO {
    
    private String label;
    private String type; // WORKFLOW_FLAG, FORM_FIELD, CASE_DATA_FIELD, CASE_FILTER, DOCUMENT_CONDITION, FORM_CONDITION
    private String flagName; // For WORKFLOW_FLAG type
    private String moduleType; // For FORM_FIELD / FORM_CONDITION / DOCUMENT_CONDITION type
    private String fieldName; // For FORM_FIELD or CASE_DATA_FIELD type
    /** For DOCUMENT_CONDITION: template ids this condition refers to. */
    private List<Long> documentTemplateIds;
    /** For FORM_CONDITION: specific permission form id (from allowedFormIds). */
    private Long formId;
    private Boolean required;
    private Boolean passed;
    private String message; // User-friendly message explaining the condition
}
