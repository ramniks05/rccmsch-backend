package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.entity.Otp;
import in.gov.manipur.rccms.entity.User;
import in.gov.manipur.rccms.exception.InvalidCredentialsException;
import in.gov.manipur.rccms.repository.OtpRepository;
import in.gov.manipur.rccms.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final SmsService smsService;
    private static final Random random = new Random();
    // Rate limiting disabled - OTP can be sent freely
    // private static final int MAX_OTP_REQUESTS_PER_15_MIN = 3;
    private static final int OTP_EXPIRY_MINUTES = 5;

    /**
     * Generate and send OTP for mobile number
     * Includes rate limiting (max 3 requests per 15 minutes)
     * By default, requires user to be active (for login scenarios)
     * 
     * @param mobileNumber Mobile number
     * @param userType User type (CITIZEN or OPERATOR)
     * @return Generated OTP code
     */
    public String generateOtp(String mobileNumber, User.UserType userType) {
        return generateOtp(mobileNumber, userType, false);
    }

    /**
     * Generate and send OTP for mobile number
     * Includes rate limiting (max 3 requests per 15 minutes)
     * 
     * @param mobileNumber Mobile number
     * @param userType User type (CITIZEN or OPERATOR)
     * @param allowInactive If true, allows OTP generation for inactive users (for registration flow)
     * @return Generated OTP code
     */
    public String generateOtp(String mobileNumber, User.UserType userType, boolean allowInactive) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number cannot be null or empty");
        }

        String trimmedMobile = mobileNumber.trim();
        Otp.UserType otpUserType = convertUserType(userType);

        // Rate limiting disabled - OTP can be sent freely
        // (Rate limiting can be enabled later if needed)
        // LocalDateTime since = LocalDateTime.now().minusMinutes(15);
        // long recentRequests = otpRepository.countRecentOtpRequests(trimmedMobile, otpUserType, since);
        // if (recentRequests >= MAX_OTP_REQUESTS_PER_15_MIN) {
        //     log.warn("Rate limit exceeded for mobile: {} (userType: {})", maskMobile(trimmedMobile), userType);
        //     throw new TooManyRequestsException("Too many OTP requests. Please try again after 15 minutes.");
        // }

        // Verify user exists with this mobile number
        Optional<User> userOpt = userRepository.findByMobileNumber(trimmedMobile);
        if (userOpt.isEmpty()) {
            // If allowInactive is true, this is for registration verification
            // User should exist after registration, but allow OTP generation anyway
            // (user might be in process of registration or transaction not committed yet)
            if (allowInactive) {
                log.warn("User not found for registration verification - mobile: {} (may be in registration process)", maskMobile(trimmedMobile));
                // Still allow OTP generation for registration verification
                // The OTP will be stored and can be verified later when user exists
            } else {
                log.warn("OTP request failed: User not found with mobile: {}", maskMobile(trimmedMobile));
                throw new InvalidCredentialsException("Mobile number not registered");
            }
        }

        // Only verify user details if user exists
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Verify user type matches
            if (!user.getUserType().equals(userType)) {
                log.warn("OTP request failed: User type mismatch for mobile: {}", maskMobile(trimmedMobile));
                throw new InvalidCredentialsException("Invalid user type");
            }

            // Check if user is active (skip check if allowInactive is true for registration flow)
            if (!allowInactive && !user.getIsActive()) {
                log.warn("OTP request failed: User account is inactive for mobile: {}", maskMobile(trimmedMobile));
                throw new InvalidCredentialsException("Account is inactive. Please contact support.");
            }
        }

        // Generate OTP
        String otpCode = generateOtpCode();
        Otp otp = new Otp();
        otp.setMobileNumber(trimmedMobile);
        otp.setUserType(otpUserType);
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
        log.info("USER TYPE: {}", userType);
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
    public boolean verifyOtp(String mobileNumber, String otpCode, User.UserType userType) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number cannot be null or empty");
        }
        if (otpCode == null || otpCode.trim().isEmpty()) {
            throw new IllegalArgumentException("OTP code cannot be null or empty");
        }

        String trimmedMobile = mobileNumber.trim();
        String trimmedOtp = otpCode.trim();
        LocalDateTime now = LocalDateTime.now();
        Otp.UserType otpUserType = convertUserType(userType);

        Optional<Otp> otpOpt = otpRepository.findValidOtpByMobileNumber(
                trimmedMobile, 
                trimmedOtp, 
                otpUserType, 
                now
        );

        return otpOpt.isPresent();
    }

    /**
     * Mark OTP as used after successful verification
     */
    public void markOtpAsUsed(String mobileNumber, String otpCode, User.UserType userType) {
        String trimmedMobile = mobileNumber.trim();
        String trimmedOtp = otpCode.trim();
        LocalDateTime now = LocalDateTime.now();
        Otp.UserType otpUserType = convertUserType(userType);

        Optional<Otp> otpOpt = otpRepository.findValidOtpByMobileNumber(
                trimmedMobile, 
                trimmedOtp, 
                otpUserType, 
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
     * Convert User.UserType to Otp.UserType
     */
    private Otp.UserType convertUserType(User.UserType userType) {
        return Otp.UserType.valueOf(userType.name());
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
