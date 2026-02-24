package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Password Reset Request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordDTO {

    @NotBlank(message = "UserID is required")
    private String userid;

    @NotBlank(message = "New password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
             message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one number, and one special character")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
