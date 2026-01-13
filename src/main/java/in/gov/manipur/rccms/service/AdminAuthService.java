package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.AdminLoginDTO;
import in.gov.manipur.rccms.dto.AuthResponseDTO;
import in.gov.manipur.rccms.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Admin Authentication Service
 * Handles admin login with default credentials
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final JwtService jwtService;

    // Default admin credentials (should be changed in production via environment variables)
    @Value("${app.admin.username:admin}")
    private String defaultAdminUsername;

    @Value("${app.admin.password:admin@123}")
    private String defaultAdminPassword;

    /**
     * Admin login with default credentials
     */
    public AuthResponseDTO adminLogin(AdminLoginDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }

        log.info("Admin login attempt for username: {}", request.getUsername());

        // Validate credentials against default admin credentials
        if (!defaultAdminUsername.equals(request.getUsername()) || 
            !defaultAdminPassword.equals(request.getPassword())) {
            log.warn("Admin login failed: Invalid credentials for username: {}", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Generate JWT token for admin
        // Using admin username and role SUPER_ADMIN
        String accessToken = jwtService.generateAdminToken(request.getUsername(), "SUPER_ADMIN");
        String refreshToken = jwtService.generateRefreshToken(0L, request.getUsername()); // Using 0L as admin ID

        log.info("Admin login successful for username: {}", request.getUsername());

        return AuthResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(0L) // Admin user ID (0 for default admin)
                .citizenType(null) // Not applicable for admin
                .email(null)
                .mobileNumber(null)
                .expiresIn(3600) // 1 hour in seconds
                .build();
    }
}

