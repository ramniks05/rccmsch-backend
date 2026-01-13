package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.AdminUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Role Master
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleMasterDTO {
    private Long id;
    private String roleCode;
    private String roleName;
    private AdminUnit.UnitLevel unitLevel;
    private String description;
}

