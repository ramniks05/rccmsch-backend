package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Post-Based Login Request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostBasedLoginDTO {

    @NotBlank(message = "UserID is required")
    private String userid; // Format: ROLE@LGD

    @NotBlank(message = "Password is required")
    private String password;
}

