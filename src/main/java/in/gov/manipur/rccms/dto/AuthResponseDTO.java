package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.Citizen;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Authentication Response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String refreshToken;
    private Long userId;
    private Citizen.CitizenType citizenType; // For citizen login
    private String email;
    private String mobileNumber;
    private Integer expiresIn; // in seconds
}

