package in.gov.manipur.rccms.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModuleFormSubmissionDTO {
    private Long id;
    private Long caseId;
    private Long caseNatureId;
    private String moduleType;
    private String formData;
    private Long submittedByOfficerId;
    private LocalDateTime submittedAt;
    private String remarks;
}

