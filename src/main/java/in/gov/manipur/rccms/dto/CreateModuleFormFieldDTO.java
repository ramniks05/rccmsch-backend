package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateModuleFormFieldDTO {
    @NotNull
    private Long caseNatureId;

    private Long caseTypeId; // optional override per case type

    @NotBlank
    private String moduleType;

    @NotBlank
    private String fieldName;

    @NotBlank
    private String fieldLabel;

    @NotBlank
    private String fieldType;

    private Boolean isRequired;
    private String validationRules;
    private Integer displayOrder;
    private Boolean isActive;
    private String defaultValue;
    private String fieldOptions;
    private String itemSchema;
    private String placeholder;
    private String helpText;
    private String dataSource;
    private String dependsOnField;
    private String dependencyCondition;
    private String conditionalLogic;
}

