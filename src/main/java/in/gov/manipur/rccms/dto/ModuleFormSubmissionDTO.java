package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.ModuleType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModuleFormSubmissionDTO {
    private Long id;
    private Long caseId;
    private Long caseNatureId;
    private ModuleType moduleType;
    private String formData;
    private Long submittedByOfficerId;
    private LocalDateTime submittedAt;
    private String remarks;
}

