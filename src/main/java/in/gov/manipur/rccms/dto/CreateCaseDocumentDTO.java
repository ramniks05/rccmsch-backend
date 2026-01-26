package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.DocumentStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateCaseDocumentDTO {
    private Long templateId;
    @NotBlank
    private String contentHtml;
    private String contentData; // JSON
    private DocumentStatus status;
    private String remarks;
}

