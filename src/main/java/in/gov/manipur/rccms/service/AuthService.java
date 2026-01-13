package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.AuthResponseDTO;
import in.gov.manipur.rccms.dto.LoginRequestDTO;
import in.gov.manipur.rccms.dto.OtpVerificationDTO;
import in.gov.manipur.rccms.entity.Citizen;
import in.gov.manipur.rccms.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service
 * Handles citizen authentication (password and OTP login)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final CitizenService citizenService;
    private final OtpService otpService;
    private final CaptchaService captchaService;
    private final JwtService jwtService;

    /**
     * Login with password
     */
    public AuthResponseDTO loginWithPassword(LoginRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }

        // Validate CAPTCHA
        boolean isValidCaptcha = captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptcha());
        if (!isValidCaptcha) {
            log.warn("Login failed: Invalid CAPTCHA");
            throw new InvalidCredentialsException("Invalid CAPTCHA");
        }

        // Mark CAPTCHA as used
        captchaService.markCaptchaAsUsed(request.getCaptchaId(), request.getCaptcha());

        // Find and verify citizen
        Citizen citizen = citizenService.findByEmailOrMobile(request.getUsername().trim());
        
        // Verify citizen type matches
        if (!citizen.getCitizenType().equals(request.getCitizenType())) {
            log.warn("Login failed: Citizen type mismatch for citizen ID: {}", citizen.getId());
            throw new InvalidCredentialsException("Invalid citizen type");
        }

        // Check if account is active
        if (!citizen.getIsActive()) {
            log.warn("Login failed: Account is inactive for citizen ID: {}", citizen.getId());
            throw new InvalidCredentialsException("Account is not active. Please verify your mobile number.");
        }

        // Verify password
        if (!citizenService.verifyCitizenCredentials(request.getUsername().trim(), request.getPassword()).equals(citizen)) {
            log.warn("Login failed: Invalid password for citizen ID: {}", citizen.getId());
            throw new InvalidCredentialsException("Invalid username or password");
        }

        // Generate tokens
        String accessToken = jwtService.generateToken(citizen.getId(), citizen.getEmail(), citizen.getCitizenType().name());
        String refreshToken = jwtService.generateRefreshToken(citizen.getId(), citizen.getEmail());

        log.info("Password login successful for citizen ID: {}", citizen.getId());

        return AuthResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(citizen.getId())
                .citizenType(citizen.getCitizenType())
                .email(citizen.getEmail())
                .mobileNumber(citizen.getMobileNumber())
                .expiresIn(3600) // 1 hour in seconds
                .build();
    }

    /**
     * Login with OTP
     */
    @Transactional
    public AuthResponseDTO loginWithOtp(OtpVerificationDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("OTP verification request cannot be null");
        }

        // Validate CAPTCHA
        boolean isValidCaptcha = captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptcha());
        if (!isValidCaptcha) {
            log.warn("OTP login failed: Invalid CAPTCHA");
            throw new InvalidCredentialsException("Invalid CAPTCHA");
        }

        // Mark CAPTCHA as used
        captchaService.markCaptchaAsUsed(request.getCaptchaId(), request.getCaptcha());

        // Verify OTP
        boolean isValidOtp = otpService.verifyOtp(
                request.getMobileNumber().trim(), 
                request.getOtp().trim(), 
                request.getCitizenType()
        );
        
        if (!isValidOtp) {
            log.warn("OTP login failed: Invalid OTP for mobile: {}", maskMobile(request.getMobileNumber()));
            throw new InvalidCredentialsException("Invalid or expired OTP");
        }

        // Find citizen
        Citizen citizen = citizenService.findByEmailOrMobile(request.getMobileNumber().trim());
        
        // Verify citizen type matches
        if (!citizen.getCitizenType().equals(request.getCitizenType())) {
            log.warn("OTP login failed: Citizen type mismatch for citizen ID: {}", citizen.getId());
            throw new InvalidCredentialsException("Invalid citizen type");
        }

        // Check if account is active
        if (!citizen.getIsActive()) {
            log.warn("OTP login failed: Account is inactive for citizen ID: {}", citizen.getId());
            throw new InvalidCredentialsException("Account is not active. Please contact support.");
        }

        // Mark OTP as used
        otpService.markOtpAsUsed(request.getMobileNumber().trim(), request.getOtp().trim(), request.getCitizenType());

        // Generate tokens
        String accessToken = jwtService.generateToken(citizen.getId(), citizen.getEmail(), citizen.getCitizenType().name());
        String refreshToken = jwtService.generateRefreshToken(citizen.getId(), citizen.getEmail());

        log.info("OTP login successful for citizen ID: {}", citizen.getId());

        return AuthResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(citizen.getId())
                .citizenType(citizen.getCitizenType())
                .email(citizen.getEmail())
                .mobileNumber(citizen.getMobileNumber())
                .expiresIn(3600) // 1 hour in seconds
                .build();
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponseDTO refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Refresh token cannot be null or empty");
        }

        // Validate refresh token
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        // Extract citizen info from refresh token
        Long citizenId = jwtService.extractUserId(refreshToken);
        String username = jwtService.extractUsername(refreshToken);
        
        Citizen citizen = citizenService.findById(citizenId);

        // Generate new access token
        String newAccessToken = jwtService.generateToken(citizen.getId(), username, citizen.getCitizenType().name());

        log.info("Token refreshed for citizen ID: {}", citizenId);

        return AuthResponseDTO.builder()
                .token(newAccessToken)
                .refreshToken(refreshToken) // Return same refresh token
                .userId(citizen.getId())
                .citizenType(citizen.getCitizenType())
                .email(citizen.getEmail())
                .mobileNumber(citizen.getMobileNumber())
                .expiresIn(3600)
                .build();
    }

    /**
     * Mask mobile for logging
     */
    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 10) {
            return "****";
        }
        return mobile.substring(0, 2) + "****" + mobile.substring(8);
    }
}

