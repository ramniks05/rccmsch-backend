package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.ModuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDocumentTemplateDTO {
    @NotNull
    private Long caseNatureId;

    private Long caseTypeId; // optional override per case type

    @NotNull
    private ModuleType moduleType;

    @NotBlank
    private String templateName;

    private String templateHtml;
    private String templateData; // JSON
    private Integer version;
    private Boolean allowEditAfterSign;
    private Boolean isActive;
}

