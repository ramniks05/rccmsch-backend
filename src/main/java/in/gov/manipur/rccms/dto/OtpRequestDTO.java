package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.Citizen;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for OTP Request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequestDTO {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be 10 digits starting with 6-9")
    private String mobileNumber;

    @NotNull(message = "Citizen type is required")
    private Citizen.CitizenType citizenType;
}

