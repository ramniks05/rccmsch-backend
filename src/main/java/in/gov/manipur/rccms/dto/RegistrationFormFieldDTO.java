package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.RegistrationFormField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Registration Form Field
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationFormFieldDTO {
    private Long id;
    private RegistrationFormField.RegistrationType registrationType;
    private String fieldName;
    private String fieldLabel;
    private String fieldType;
    private Boolean isRequired;
    private String validationRules;
    private Integer displayOrder;
    private Boolean isActive;
    private String defaultValue;
    private String fieldOptions;
    private String dataSource;
    private String placeholder;
    private String helpText;
    private String fieldGroup;
    private String conditionalLogic;
}
