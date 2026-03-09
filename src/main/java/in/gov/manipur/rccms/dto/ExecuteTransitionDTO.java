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
}

