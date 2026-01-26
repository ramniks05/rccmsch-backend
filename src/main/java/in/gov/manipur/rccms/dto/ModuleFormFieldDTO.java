package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.ModuleType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ModuleFormFieldDTO {
    private Long id;
    private Long caseNatureId;
    private String caseNatureCode;
    private String caseNatureName;
    private Long caseTypeId;
    private String caseTypeCode;
    private String caseTypeName;
    private ModuleType moduleType;
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private Boolean isRequired;
    private String validationRules;
    private Integer displayOrder;
    private Boolean isActive;
    private String defaultValue;
    private String fieldOptions;
    private String placeholder;
    private String helpText;
    private String dataSource;
    private String dependsOnField;
    private String dependencyCondition;
    private String conditionalLogic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

