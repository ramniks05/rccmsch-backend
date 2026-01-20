package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating Workflow State
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStateDTO {
    
    @NotBlank(message = "State code is required")
    @Size(max = 50, message = "State code must not exceed 50 characters")
    private String stateCode;
    
    @NotBlank(message = "State name is required")
    @Size(max = 200, message = "State name must not exceed 200 characters")
    private String stateName;
    
    @NotNull(message = "State order is required")
    private Integer stateOrder;
    
    private Boolean isInitialState = false;
    
    private Boolean isFinalState = false;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
