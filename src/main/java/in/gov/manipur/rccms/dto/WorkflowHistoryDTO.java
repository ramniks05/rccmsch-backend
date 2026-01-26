package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Workflow History
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowHistoryDTO {
    private Long id;
    private Long caseId;
    private Long instanceId;
    
    // Transition information
    private Long transitionId;
    private String transitionCode;
    private String transitionName;
    
    // State information
    private Long fromStateId;
    private String fromStateCode;
    private String fromStateName;
    
    private Long toStateId;
    private String toStateCode;
    private String toStateName;
    
    // Performed by information
    private Long performedByOfficerId;
    private String performedByOfficerName;
    private String performedByRole;
    
    // Unit information
    private Long performedAtUnitId;
    private String performedAtUnitName;
    
    // Other fields
    private String comments;
    private String metadata;
    private LocalDateTime performedAt;
}
