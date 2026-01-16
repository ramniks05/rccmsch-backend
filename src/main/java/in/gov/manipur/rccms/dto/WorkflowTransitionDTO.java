package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Workflow Transition
 */
@Data
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
}

