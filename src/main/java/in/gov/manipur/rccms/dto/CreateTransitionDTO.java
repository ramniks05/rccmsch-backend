package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating Workflow Transition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransitionDTO {
    
    @NotBlank(message = "Transition code is required")
    @Size(max = 50, message = "Transition code must not exceed 50 characters")
    private String transitionCode;
    
    @NotBlank(message = "Transition name is required")
    @Size(max = 200, message = "Transition name must not exceed 200 characters")
    private String transitionName;
    
    @NotNull(message = "From state ID is required")
    private Long fromStateId;
    
    @NotNull(message = "To state ID is required")
    private Long toStateId;
    
    private Boolean requiresComment = false;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private Boolean isActive = true;
}
