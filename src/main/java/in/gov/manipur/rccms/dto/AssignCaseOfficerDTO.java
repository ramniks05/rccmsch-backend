package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for assigning case to a specific officer
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignCaseOfficerDTO {
    
    @NotNull(message = "Officer ID is required")
    private Long officerId;
    
    @NotNull(message = "Role code is required")
    private String roleCode;
}
