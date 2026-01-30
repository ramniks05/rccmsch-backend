package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for Transition Conditions
 * Aggregates all conditions from all permissions for a transition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionConditionsDTO {
    private Long transitionId;
    private String transitionCode;
    private String transitionName;
    private List<PermissionConditionsDTO> permissions;
    
    /**
     * Aggregated conditions from all permissions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionConditionsDTO {
        private Long permissionId;
        private String roleCode;
        private String unitLevel;
        private String hierarchyRule;
        private Map<String, Object> conditions; // Parsed JSON conditions
        private Boolean canInitiate;
        private Boolean isActive;
    }
}
