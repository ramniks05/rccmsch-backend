package in.gov.manipur.rccms.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentTemplateDTO {
    private Long id;
    private Long caseNatureId;
    private String caseNatureCode;
    private String caseNatureName;
    private Long caseTypeId;
    private String caseTypeCode;
    private String caseTypeName;
    private String moduleType;
    private String templateName;
    private String templateHtml;
    private String templateData;
    private Integer version;
    private Boolean allowEditAfterSign;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

