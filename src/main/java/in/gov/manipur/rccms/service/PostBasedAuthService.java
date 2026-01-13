package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.AuthResponseDTO;
import in.gov.manipur.rccms.dto.PostBasedLoginDTO;
import in.gov.manipur.rccms.entity.Officer;
import in.gov.manipur.rccms.entity.OfficerDaHistory;
import in.gov.manipur.rccms.exception.InvalidCredentialsException;
import in.gov.manipur.rccms.repository.OfficerDaHistoryRepository;
import in.gov.manipur.rccms.repository.OfficerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Post-Based Authentication Service
 * Handles authentication for government employees using UserID (ROLE@LGD) format
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostBasedAuthService {

    private final OfficerDaHistoryRepository postingRepository;
    private final OfficerRepository officerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Login with UserID and password (post-based login)
     * UserID format: ROLE_CODE@UNIT_LGD_CODE
     */
    @Transactional
    public AuthResponseDTO loginWithPostBasedCredentials(PostBasedLoginDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }

        log.info("Post-based login attempt for UserID: {}", request.getUserid());

        // Find active posting by UserID
        OfficerDaHistory posting = postingRepository.findByPostingUseridAndIsCurrentTrue(request.getUserid())
                .orElseThrow(() -> {
                    log.warn("Login failed: Active posting not found for UserID: {}", request.getUserid());
                    return new InvalidCredentialsException("Invalid UserID or posting is not active");
                });

        // Get officer (person)
        Officer officer = posting.getOfficer();
        if (officer == null) {
            throw new RuntimeException("Officer not found for posting UserID: " + request.getUserid());
        }

        // Check if officer is active
        if (!officer.getIsActive()) {
            log.warn("Login failed: Officer account is inactive for UserID: {}", request.getUserid());
            throw new InvalidCredentialsException("Officer account is not active");
        }

        // Verify password
        if (officer.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), officer.getPasswordHash())) {
            log.warn("Login failed: Invalid password for UserID: {}", request.getUserid());
            throw new InvalidCredentialsException("Invalid UserID or password");
        }

        // Check if password reset is required (first login)
        if (officer.getIsPasswordResetRequired()) {
            log.info("Password reset required for UserID: {}", request.getUserid());
            // Return response indicating password reset is required
            // Frontend should redirect to password reset page
            throw new InvalidCredentialsException("Password reset required. Please reset your password.");
        }

        // Generate JWT token with posting information
        String accessToken = jwtService.generatePostBasedToken(
                posting.getPostingUserid(),
                posting.getRoleCode(),
                posting.getUnitId(),
                posting.getUnit() != null ? posting.getUnit().getUnitLevel().name() : null,
                officer.getId()
        );
        
        String refreshToken = jwtService.generateRefreshToken(officer.getId(), officer.getEmail());

        log.info("Post-based login successful for UserID: {}, Officer: {}", request.getUserid(), officer.getFullName());

        return AuthResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(officer.getId())
                .citizenType(null) // Not applicable for post-based login
                .email(officer.getEmail())
                .mobileNumber(officer.getMobileNo())
                .expiresIn(3600) // 1 hour in seconds
                .build();
    }

    /**
     * Reset password (for first login)
     */
    @Transactional
    public void resetPassword(String userid, String newPassword) {
        // Find active posting
        OfficerDaHistory posting = postingRepository.findByPostingUseridAndIsCurrentTrue(userid)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid UserID or posting is not active"));

        Officer officer = posting.getOfficer();
        if (officer == null) {
            throw new RuntimeException("Officer not found for posting UserID: " + userid);
        }

        // Update password
        String passwordHash = passwordEncoder.encode(newPassword);
        officer.setPasswordHash(passwordHash);
        officer.setIsPasswordResetRequired(false);
        officerRepository.save(officer);

        log.info("Password reset successful for UserID: {}", userid);
    }

    /**
     * Verify mobile number with OTP (for first login profile update)
     */
    @Transactional
    public void verifyMobileWithOtp(String userid, String otp) {
        // Find active posting
        OfficerDaHistory posting = postingRepository.findByPostingUseridAndIsCurrentTrue(userid)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid UserID or posting is not active"));

        Officer officer = posting.getOfficer();
        if (officer == null) {
            throw new RuntimeException("Officer not found for posting UserID: " + userid);
        }

        // Note: OTP verification for officers may need separate implementation
        // For now, this is a placeholder - you may need to create a separate OTP service for officers
        // or extend the existing OtpService to handle officer OTPs
        
        // Verify mobile (simplified - OTP verification for officers can be added later)
        officer.setIsMobileVerified(true);
        officerRepository.save(officer);

        log.info("Mobile number verified for UserID: {}", userid);
    }
}

