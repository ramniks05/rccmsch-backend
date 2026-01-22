package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.AuthResponseDTO;
import in.gov.manipur.rccms.dto.LoginRequestDTO;
import in.gov.manipur.rccms.dto.OtpVerificationDTO;
import in.gov.manipur.rccms.entity.Citizen;
import in.gov.manipur.rccms.entity.Lawyer;
import in.gov.manipur.rccms.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lawyer Authentication Service
 * Handles lawyer authentication (password and OTP login)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LawyerAuthService {

    private final LawyerService lawyerService;
    private final OtpService otpService;
    private final CaptchaService captchaService;
    private final JwtService jwtService;

    /**
     * Login with password
     */
    @Transactional
    public AuthResponseDTO loginWithPassword(LoginRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }

        boolean isValidCaptcha = captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptcha());
        if (!isValidCaptcha) {
            throw new InvalidCredentialsException("Invalid CAPTCHA");
        }
        captchaService.markCaptchaAsUsed(request.getCaptchaId(), request.getCaptcha());

        Lawyer lawyer = lawyerService.findByEmailOrMobile(request.getUsername().trim());
        lawyerService.verifyLawyerCredentials(request.getUsername().trim(), request.getPassword());

        String accessToken = jwtService.generateToken(lawyer.getId(), lawyer.getEmail(), Citizen.CitizenType.LAWYER.name());
        String refreshToken = jwtService.generateRefreshToken(lawyer.getId(), lawyer.getEmail());

        return AuthResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(lawyer.getId())
                .citizenType(Citizen.CitizenType.LAWYER)
                .email(lawyer.getEmail())
                .mobileNumber(lawyer.getMobileNumber())
                .expiresIn(3600)
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

        boolean isValidCaptcha = captchaService.validateCaptcha(request.getCaptchaId(), request.getCaptcha());
        if (!isValidCaptcha) {
            throw new InvalidCredentialsException("Invalid CAPTCHA");
        }
        captchaService.markCaptchaAsUsed(request.getCaptchaId(), request.getCaptcha());

        boolean isValidOtp = otpService.verifyOtp(
                request.getMobileNumber().trim(),
                request.getOtp().trim(),
                Citizen.CitizenType.LAWYER
        );
        if (!isValidOtp) {
            throw new InvalidCredentialsException("Invalid or expired OTP");
        }

        Lawyer lawyer = lawyerService.findByEmailOrMobile(request.getMobileNumber().trim());
        if (!lawyer.getIsActive()) {
            throw new InvalidCredentialsException("Account is not active. Please contact support.");
        }

        otpService.markOtpAsUsed(request.getMobileNumber().trim(), request.getOtp().trim(), Citizen.CitizenType.LAWYER);

        String accessToken = jwtService.generateToken(lawyer.getId(), lawyer.getEmail(), Citizen.CitizenType.LAWYER.name());
        String refreshToken = jwtService.generateRefreshToken(lawyer.getId(), lawyer.getEmail());

        return AuthResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(lawyer.getId())
                .citizenType(Citizen.CitizenType.LAWYER)
                .email(lawyer.getEmail())
                .mobileNumber(lawyer.getMobileNumber())
                .expiresIn(3600)
                .build();
    }
}
