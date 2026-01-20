package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.AdminUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating Workflow Permission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionDTO {
    
    @NotBlank(message = "Role code is required")
    @Size(max = 50, message = "Role code must not exceed 50 characters")
    private String roleCode;
    
    private AdminUnit.UnitLevel unitLevel; // null means all levels
    
    private Boolean canInitiate = false;
    
    private Boolean canApprove = false;
    
    @Size(max = 50, message = "Hierarchy rule must not exceed 50 characters")
    private String hierarchyRule; // "SAME_UNIT", "PARENT_UNIT", "ANY_UNIT", "SUPERVISOR"
    
    private String conditions; // JSON string for additional conditions
    
    private Boolean isActive = true;
}
