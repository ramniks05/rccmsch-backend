package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Workflow Transition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransitionDTO {
    private Long id;
    private String transitionCode;
    private String transitionName;
    private String fromStateCode;
    private String toStateCode;
    private Boolean requiresComment;
    private String description;
    
    // Checklist showing conditions that must be met
    private TransitionChecklistDTO checklist;
    
    // Form schema if this transition requires a form (e.g., HEARING form)
    private ModuleFormSchemaDTO formSchema;
}

