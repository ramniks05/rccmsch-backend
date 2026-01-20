package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Workflow State response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStateDTO {
    private Long id;
    private Long workflowId;
    private String workflowCode;
    private String stateCode;
    private String stateName;
    private Integer stateOrder;
    private Boolean isInitialState;
    private Boolean isFinalState;
    private String description;
}
