package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for executing workflow transition
 * Note: caseId is passed as path parameter, not in DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteTransitionDTO {
    
    @NotBlank(message = "Transition code is required")
    private String transitionCode;
    
    private String comments; // Optional comments for the transition
    
    /**
     * Optional officer ID for manual assignment
     * Used for transitions like REQUEST_FIELD_REPORT where a specific officer needs to be assigned
     * If provided, the system will assign the case to this officer instead of auto-assigning
     */
    private Long assignedOfficerId;
}

