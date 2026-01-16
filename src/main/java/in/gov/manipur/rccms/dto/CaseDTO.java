package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Long assignedToUnitId;
    private String assignedToUnitName;
}

