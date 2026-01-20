package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.AdminUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Workflow Permission response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowPermissionDTO {
    private Long id;
    private Long transitionId;
    private String transitionCode;
    private String roleCode;
    private AdminUnit.UnitLevel unitLevel;
    private Boolean canInitiate;
    private Boolean canApprove;
    private String hierarchyRule;
    private String conditions;
    private Boolean isActive;
}
