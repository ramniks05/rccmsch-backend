package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.RegistrationFormField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Registration Form Schema
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationFormSchemaDTO {
    private RegistrationFormField.RegistrationType registrationType;
    private List<RegistrationFormFieldDTO> fields;
}
