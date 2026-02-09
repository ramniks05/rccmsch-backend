package in.gov.manipur.rccms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Form Field Definition
 * Simplified for frontend binding - excludes null values and unnecessary fields
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FormFieldDefinitionDTO {
    private Long id;
    private Long caseTypeId;
    private String caseTypeName;
    private String caseTypeCode;
    private String fieldName;
    private String fieldLabel;
    private String fieldType; // TEXT, TEXTAREA, RICH_TEXT, NUMBER, DATE, DATETIME, SELECT, MULTISELECT, CHECKBOX, RADIO, FILE
    
    @Builder.Default
    private Boolean isRequired = false;
    
    private String validationRules; // JSON string
    private Integer displayOrder;
    
    @Builder.Default
    private Boolean isActive = true;
    
    private String defaultValue;
    private String fieldOptions; // JSON string (static options)
    private String placeholder;
    private String helpText;
    private String fieldGroup; // Group code
    private String groupLabel; // Display name for the group
    private Integer groupDisplayOrder; // Order of the group
    private String dataSource; // JSON: {type:"ADMIN_UNITS", level:"DISTRICT", parentField:"stateId", apiEndpoint:"..."}
    private String dependsOnField; // Field name this field depends on
    private String dependencyCondition; // JSON: {operator:"equals", value:"expectedValue"}
    private String conditionalLogic; // JSON: {showIf: {field: "fieldName", operator: "equals", value: "expectedValue"}}
    
    // Exclude timestamps from public API response
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime createdAt;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime updatedAt;
}

