package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating Workflow Definition
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowDTO {
    
    @NotBlank(message = "Workflow code is required")
    @Size(max = 50, message = "Workflow code must not exceed 50 characters")
    private String workflowCode;
    
    @NotBlank(message = "Workflow name is required")
    @Size(max = 200, message = "Workflow name must not exceed 200 characters")
    private String workflowName;
    
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
    
    private Boolean isActive = true;
}
