package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new form field
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFormFieldDTO {
    
    @NotNull(message = "Case type ID is required")
    private Long caseTypeId; // Case Type ID (NEW_FILE, APPEAL, REVISION, etc.)
    
    @NotBlank(message = "Field name is required")
    private String fieldName;
    
    @NotBlank(message = "Field label is required")
    private String fieldLabel;
    
    @NotBlank(message = "Field type is required")
    private String fieldType; // TEXT, TEXTAREA, RICH_TEXT, NUMBER, DATE, DATETIME, SELECT, MULTISELECT, CHECKBOX, RADIO, FILE
    
    private Boolean isRequired = false;
    
    private String validationRules; // JSON string
    
    private Integer displayOrder = 0;
    
    private Boolean isActive = true;
    
    private String defaultValue;
    
    private String fieldOptions; // JSON string for SELECT/RADIO (static options)
    
    private String placeholder;
    
    private String helpText;
    
    private String fieldGroup; // Group code (references FormFieldGroup.groupCode)
    
    private String dataSource; // JSON: {type:"ADMIN_UNITS", level:"DISTRICT", parentField:"stateId", apiEndpoint:"..."}
    
    private String dependsOnField; // Field name this field depends on for conditional dropdowns
    
    private String dependencyCondition; // JSON: {operator:"equals", value:"expectedValue"}
    
    private String conditionalLogic; // JSON: {showIf: {field: "fieldName", operator: "equals", value: "expectedValue"}}

    // Optional on-change API and response mapping (used by frontend to configure dependent detail fetch)
    private String onChangeApi; // JSON string
    private String onChangeResponseMapping; // JSON string
}

