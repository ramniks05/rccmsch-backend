package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.CourtLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for CaseType entity (Previously CaseNatureDTO)
 * Represents case types like NEW_FILE, APPEAL, REVISION, etc.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseTypeDTO {
    private Long id;
    private Long caseNatureId;
    private String caseNatureName;
    private String caseNatureCode;
    private String typeCode;
    private String typeName;
    private CourtLevel courtLevel;
    private List<String> courtTypes; // Parsed from JSON string
    private CourtLevel fromLevel;
    private Boolean isAppeal;
    private Integer appealOrder;
    private String description;
    private String workflowCode; // Links to workflow_definition.workflow_code
    private Boolean isActive;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
