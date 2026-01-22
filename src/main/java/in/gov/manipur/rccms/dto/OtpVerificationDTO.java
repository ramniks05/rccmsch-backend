package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.Citizen;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for OTP Verification Request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpVerificationDTO {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be 10 digits starting with 6-9")
    private String mobileNumber;

    @NotBlank(message = "OTP code is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
    private String otp;

    @NotBlank(message = "CAPTCHA is required")
    @Size(min = 4, max = 10, message = "CAPTCHA must be between 4 and 10 characters")
    private String captcha;

    @NotBlank(message = "CAPTCHA ID is required")
    private String captchaId;

    private Citizen.CitizenType citizenType;
}

