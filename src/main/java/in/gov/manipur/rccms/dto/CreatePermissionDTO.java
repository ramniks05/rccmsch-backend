package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.AdminUnit;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for creating/updating Workflow Permission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionDTO {

    /** Role from role_master (preferred). When set, role_code is derived from role_master. */
    private Long roleId;

    @Size(max = 50, message = "Role code must not exceed 50 characters")
    private String roleCode; // required if roleId not set; otherwise derived from role_master

    private AdminUnit.UnitLevel unitLevel; // null means all levels
    
    private Boolean canInitiate = false;
    
    private Boolean canApprove = false;
    
    @Size(max = 50, message = "Hierarchy rule must not exceed 50 characters")
    private String hierarchyRule; // "SAME_UNIT", "PARENT_UNIT", "ANY_UNIT", "SUPERVISOR"
    
    private String conditions; // JSON string for additional conditions
    
    private Boolean isActive = true;

    /** Form IDs (case type IDs with form fields) this permission allows. From GET /permission-forms. */
    private List<Long> allowedFormIds;
    /** Document template IDs this permission allows. From GET /permission-documents. */
    private List<Long> allowedDocumentIds;
    /** Whether this permission allows document draft (save as draft). */
    private Boolean allowDocumentDraft;
    /** Whether this permission allows document save and sign. */
    private Boolean allowDocumentSaveAndSign;
    /** Per-document allowed stages (e.g. [{"documentId":5,"stages":["SAVE_AND_SIGN"]}]). */
    private List<AllowedDocumentStageEntryDTO> allowedDocumentStages;
}
