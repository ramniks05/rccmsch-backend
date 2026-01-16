package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Form Field Definition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormFieldDefinitionDTO {
    private Long id;
    private Long caseTypeId;
    private String caseTypeName;
    private String caseTypeCode;
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private Boolean isRequired;
    private String validationRules; // JSON string
    private Integer displayOrder;
    private Boolean isActive;
    private String defaultValue;
    private String fieldOptions; // JSON string
    private String placeholder;
    private String helpText;
    private String fieldGroup;
    private String conditionalLogic; // JSON string
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

