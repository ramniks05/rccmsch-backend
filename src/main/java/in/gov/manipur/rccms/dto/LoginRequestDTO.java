package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.Citizen;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Password Login Request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "Username (mobile number or email) is mandatory")
    @Size(min = 1, max = 100, message = "Username must be between 1 and 100 characters")
    private String username; // Can be mobile number or email

    @NotBlank(message = "Password is mandatory")
    @Size(min = 1, max = 100, message = "Password must be between 1 and 100 characters")
    private String password;

    @NotBlank(message = "CAPTCHA is required")
    @Size(min = 4, max = 10, message = "CAPTCHA must be between 4 and 10 characters")
    private String captcha;

    @NotBlank(message = "CAPTCHA ID is required")
    private String captchaId;

    @NotNull(message = "Citizen type is required")
    private Citizen.CitizenType citizenType;
}

