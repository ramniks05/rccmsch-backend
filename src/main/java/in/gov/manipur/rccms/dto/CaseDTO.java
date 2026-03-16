package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Case
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseDTO {
    private Long id;
    private String caseNumber;
    private Long caseTypeId;
    private String caseTypeName;
    private String caseTypeCode;
    private Long caseNatureId;
    private String caseNatureName;
    private String caseNatureCode;
    private Long courtId;
    private String courtName;
    private String courtCode;
    private String originalOrderLevel;
    private Long applicantId;
    private String applicantName;
    private String applicantMobile;
    private String applicantEmail;
    private Long unitId;
    private String unitName;
    private String unitCode;
    private String subject;
    private String description;
    private String status;
    private String statusName;
    private String priority;
    private LocalDate applicationDate;
    private LocalDate resolvedDate;
    private LocalDate nextHearingDate;
    private String remarks;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Workflow instance info
    private Long workflowInstanceId;
    private String workflowCode;
    private Long currentStateId;
    private String currentStateCode;
    private String currentStateName;
    private Long assignedToOfficerId;
    private String assignedToOfficerName;
    private String assignedToRole;
    private Long assignedToRoleId;
    private Long assignedToUnitId;
    private String assignedToUnitName;

    /** Roles that can perform the next transition(s). Display e.g. "Pending with: Dealing Hand, Presiding Officer". */
    private List<String> pendingWithRoleNames;
    /** Comma-separated role names for display (e.g. "Dealing Hand, Presiding Officer"). */
    private String pendingWithRolesDisplay;

    /** Form/case-specific data as JSON (e.g. applicant_name, respondent_name, area details). */
    private String caseData;

    /** Form data with field labels and groups for display (fieldName, fieldLabel, fieldGroup, groupLabel, value, displayOrder). */
    private List<FormDataDisplayItemDTO> formDataWithLabels;
}

