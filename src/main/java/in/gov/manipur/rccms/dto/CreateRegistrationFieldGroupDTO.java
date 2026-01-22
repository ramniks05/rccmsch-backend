package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.RegistrationFormField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating registration field groups
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRegistrationFieldGroupDTO {

    @NotNull(message = "Registration type is required")
    private RegistrationFormField.RegistrationType registrationType;

    @NotBlank(message = "Group code is required")
    @Size(max = 50, message = "Group code must not exceed 50 characters")
    private String groupCode;

    @NotBlank(message = "Group label is required")
    @Size(max = 100, message = "Group label must not exceed 100 characters")
    private String groupLabel;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private Integer displayOrder = 0;

    private Boolean isActive = true;
}
