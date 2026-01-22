package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.RegistrationFormField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating Registration Form Field
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRegistrationFormFieldDTO {

    @NotNull(message = "Registration type is required")
    private RegistrationFormField.RegistrationType registrationType;

    @NotBlank(message = "Field name is required")
    @Size(max = 100, message = "Field name must not exceed 100 characters")
    private String fieldName;

    @NotBlank(message = "Field label is required")
    @Size(max = 200, message = "Field label must not exceed 200 characters")
    private String fieldLabel;

    @NotBlank(message = "Field type is required")
    @Size(max = 50, message = "Field type must not exceed 50 characters")
    private String fieldType;

    private Boolean isRequired = false;

    private String validationRules;

    private Integer displayOrder = 0;

    private Boolean isActive = true;

    private String defaultValue;

    private String fieldOptions;

    private String dataSource;

    private String placeholder;

    private String helpText;

    private String fieldGroup;

    private String conditionalLogic;
}
