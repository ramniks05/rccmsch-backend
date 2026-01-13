package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.Officer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Officer (Government Employee)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfficerDTO {

    private Long id;

    @NotBlank(message = "Full name is required")
    @Size(max = 200, message = "Full name must not exceed 200 characters")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be 10 digits starting with 6-9")
    private String mobileNo;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    private Officer.AuthType authType;
    private Boolean isActive;
    private Boolean isPasswordResetRequired;
    private Boolean isMobileVerified;
}

