package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.DocumentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaseDocumentDTO {
    private Long id;
    private Long caseId;
    private Long caseNatureId;
    private String moduleType;
    private Long templateId;
    private String templateName;
    private String contentHtml;
    private String contentData;
    private DocumentStatus status;
    private Long signedByOfficerId;
    private LocalDateTime signedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

