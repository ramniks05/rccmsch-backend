package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for executing workflow transition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteTransitionDTO {
    
    @NotNull(message = "Case ID is required")
    private Long caseId;
    
    @NotBlank(message = "Transition code is required")
    private String transitionCode;
    
    private String comments; // Optional comments for the transition
}

