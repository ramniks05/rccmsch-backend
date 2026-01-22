package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.RegistrationFormField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Registration Field Group
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationFieldGroupDTO {
    private Long id;
    private RegistrationFormField.RegistrationType registrationType;
    private String groupCode;
    private String groupLabel;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;
}
