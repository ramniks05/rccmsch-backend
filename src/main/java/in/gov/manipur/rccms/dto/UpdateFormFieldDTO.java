package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating a form field
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFormFieldDTO {
    
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
    
    private String fieldGroup; // Group code (references FormFieldGroup.groupCode)
    
    private String dataSource; // JSON string
    
    private String dependsOnField;
    
    private String dependencyCondition; // JSON string
    
    private String conditionalLogic; // JSON string

    // Optional on-change API and response mapping
    private String onChangeApi; // JSON string
    private String onChangeResponseMapping; // JSON string
}

