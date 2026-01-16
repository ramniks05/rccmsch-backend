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
    private Long caseTypeId;
    
    @NotBlank(message = "Field name is required")
    private String fieldName;
    
    @NotBlank(message = "Field label is required")
    private String fieldLabel;
    
    @NotBlank(message = "Field type is required")
    private String fieldType; // TEXT, NUMBER, DATE, DATETIME, EMAIL, PHONE, TEXTAREA, SELECT, RADIO, CHECKBOX, FILE
    
    private Boolean isRequired = false;
    
    private String validationRules; // JSON string
    
    private Integer displayOrder = 0;
    
    private Boolean isActive = true;
    
    private String defaultValue;
    
    private String fieldOptions; // JSON string for SELECT/RADIO
    
    private String placeholder;
    
    private String helpText;
    
    private String fieldGroup;
    
    private String conditionalLogic; // JSON string
}

