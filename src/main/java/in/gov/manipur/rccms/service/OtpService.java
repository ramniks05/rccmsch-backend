package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.entity.Citizen;
import in.gov.manipur.rccms.entity.Otp;
import in.gov.manipur.rccms.exception.InvalidCredentialsException;
import in.gov.manipur.rccms.entity.Lawyer;
import in.gov.manipur.rccms.repository.CitizenRepository;
import in.gov.manipur.rccms.repository.LawyerRepository;
import in.gov.manipur.rccms.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

/**
 * OTP Service
 * Handles OTP generation, validation, and cleanup with rate limiting
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OtpService {

    private final OtpRepository otpRepository;
    private final CitizenRepository citizenRepository;
    private final LawyerRepository lawyerRepository;
    private final SmsService smsService;
    private static final Random random = new Random();
    private static final int OTP_EXPIRY_MINUTES = 5;

    /**
     * Generate and send OTP for mobile number
     * Includes rate limiting (max 3 requests per 15 minutes)
     * By default, requires citizen to be active (for login scenarios)
     * 
     * @param mobileNumber Mobile number
     * @param citizenType Citizen type (CITIZEN or OPERATOR)
     * @return Generated OTP code
     */
    public String generateOtp(String mobileNumber, Citizen.CitizenType citizenType) {
        return generateOtp(mobileNumber, citizenType, false);
    }

    /**
     * Generate and send OTP for mobile number
     * Includes rate limiting (max 3 requests per 15 minutes)
     * 
     * @param mobileNumber Mobile number
     * @param citizenType Citizen type (CITIZEN or OPERATOR)
     * @param allowInactive If true, allows OTP generation for inactive citizens (for registration flow)
     * @return Generated OTP code
     */
    public String generateOtp(String mobileNumber, Citizen.CitizenType citizenType, boolean allowInactive) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number cannot be null or empty");
        }

        String trimmedMobile = mobileNumber.trim();
        Otp.CitizenType otpCitizenType = convertCitizenType(citizenType);

        if (Citizen.CitizenType.LAWYER.equals(citizenType)) {
            Optional<Lawyer> lawyerOpt = lawyerRepository.findByMobileNumber(trimmedMobile);
            if (lawyerOpt.isEmpty()) {
                if (allowInactive) {
                    log.warn("Lawyer not found for registration verification - mobile: {} (may be in registration process)", maskMobile(trimmedMobile));
                } else {
                    log.warn("OTP request failed: Lawyer not found with mobile: {}", maskMobile(trimmedMobile));
                    throw new InvalidCredentialsException("Mobile number not registered");
                }
            }

            if (lawyerOpt.isPresent()) {
                Lawyer lawyer = lawyerOpt.get();
                if (!allowInactive && !lawyer.getIsActive()) {
                    log.warn("OTP request failed: Lawyer account is inactive for mobile: {}", maskMobile(trimmedMobile));
                    throw new InvalidCredentialsException("Account is inactive. Please contact support.");
                }
            }
        } else {
            // Verify citizen exists with this mobile number
            Optional<Citizen> citizenOpt = citizenRepository.findByMobileNumber(trimmedMobile);
            if (citizenOpt.isEmpty()) {
                // If allowInactive is true, this is for registration verification
                // Citizen should exist after registration, but allow OTP generation anyway
                if (allowInactive) {
                    log.warn("Citizen not found for registration verification - mobile: {} (may be in registration process)", maskMobile(trimmedMobile));
                } else {
                    log.warn("OTP request failed: Citizen not found with mobile: {}", maskMobile(trimmedMobile));
                    throw new InvalidCredentialsException("Mobile number not registered");
                }
            }

            if (citizenOpt.isPresent()) {
                Citizen citizen = citizenOpt.get();
                if (!citizen.getCitizenType().equals(citizenType)) {
                    log.warn("OTP request failed: Citizen type mismatch for mobile: {}", maskMobile(trimmedMobile));
                    throw new InvalidCredentialsException("Invalid citizen type");
                }
                if (!allowInactive && !citizen.getIsActive()) {
                    log.warn("OTP request failed: Citizen account is inactive for mobile: {}", maskMobile(trimmedMobile));
                    throw new InvalidCredentialsException("Account is inactive. Please contact support.");
                }
            }
        }

        // Generate OTP
        String otpCode = generateOtpCode();
        Otp otp = new Otp();
        otp.setMobileNumber(trimmedMobile);
        otp.setCitizenType(otpCitizenType);
        otp.setOtpCode(otpCode);
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otp.setIsUsed(false);

        otpRepository.save(otp);
        
        // Log OTP to console (DUMMY OTP - SMS API will be integrated later)
        log.info("========================================");
        log.info("=== OTP GENERATED (DUMMY) ===");
        log.info("========================================");
        log.info("MOBILE NUMBER: {}", trimmedMobile);
        log.info("OTP CODE: {}", otpCode);
        log.info("CITIZEN TYPE: {}", citizenType);
        log.info("EXPIRY: {} minutes", OTP_EXPIRY_MINUTES);
        log.info("STATUS: {}", allowInactive ? "Registration Verification" : "Login");
        log.info("========================================");
        log.info("NOTE: This is a DUMMY OTP logged to console.");
        log.info("SMS API will be integrated later.");
        log.info("========================================");

        // Send OTP via SMS service (currently logs to console - DUMMY)
        smsService.sendSms(trimmedMobile, "Your RCCMS OTP is: " + otpCode + ". Valid for " + OTP_EXPIRY_MINUTES + " minutes.");

        // Return OTP code for API response (temporary - until SMS API is integrated)
        return otpCode;
    }

    /**
     * Verify OTP code
     */
    @Transactional(readOnly = true)
    public boolean verifyOtp(String mobileNumber, String otpCode, Citizen.CitizenType citizenType) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number cannot be null or empty");
        }
        if (otpCode == null || otpCode.trim().isEmpty()) {
            throw new IllegalArgumentException("OTP code cannot be null or empty");
        }

        String trimmedMobile = mobileNumber.trim();
        String trimmedOtp = otpCode.trim();
        LocalDateTime now = LocalDateTime.now();
        Otp.CitizenType otpCitizenType = convertCitizenType(citizenType);

        Optional<Otp> otpOpt = otpRepository.findValidOtpByMobileNumber(
                trimmedMobile, 
                trimmedOtp, 
                otpCitizenType, 
                now
        );

        return otpOpt.isPresent();
    }

    /**
     * Mark OTP as used after successful verification
     */
    public void markOtpAsUsed(String mobileNumber, String otpCode, Citizen.CitizenType citizenType) {
        String trimmedMobile = mobileNumber.trim();
        String trimmedOtp = otpCode.trim();
        LocalDateTime now = LocalDateTime.now();
        Otp.CitizenType otpCitizenType = convertCitizenType(citizenType);

        Optional<Otp> otpOpt = otpRepository.findValidOtpByMobileNumber(
                trimmedMobile, 
                trimmedOtp, 
                otpCitizenType, 
                now
        );

        if (otpOpt.isPresent()) {
            otpRepository.markAsUsed(otpOpt.get().getId());
            log.debug("OTP marked as used for mobile: {}", maskMobile(trimmedMobile));
        }
    }

    /**
     * Generate 6-digit OTP code
     */
    private String generateOtpCode() {
        int otp = 100000 + random.nextInt(900000); // Generates 6-digit number (100000-999999)
        return String.valueOf(otp);
    }

    /**
     * Convert Citizen.CitizenType to Otp.CitizenType
     */
    private Otp.CitizenType convertCitizenType(Citizen.CitizenType citizenType) {
        return Otp.CitizenType.valueOf(citizenType.name());
    }

    /**
     * Mask mobile number for logging
     */
    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 10) {
            return "****";
        }
        return mobile.substring(0, 2) + "****" + mobile.substring(8);
    }

    /**
     * Clean up expired OTPs (runs every hour)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    @Transactional
    public void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpRepository.deleteExpiredOtps(now);
        log.debug("Cleaned up expired OTPs");
    }
}
