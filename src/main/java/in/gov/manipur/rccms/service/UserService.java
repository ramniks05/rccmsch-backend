package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.UserRegistrationDTO;
import in.gov.manipur.rccms.entity.User;
import in.gov.manipur.rccms.exception.DuplicateUserException;
import in.gov.manipur.rccms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Service
 * Handles user registration and management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionService encryptionService;
    private final OtpService otpService;

    /**
     * Register a new citizen
     * @param dto registration DTO
     * @return Map containing userId and otpCode (temporary - until SMS API is integrated)
     * @throws DuplicateUserException if email/mobile/aadhar already exists
     */
    public java.util.Map<String, Object> registerCitizen(UserRegistrationDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Registration data cannot be null");
        }

        // Validate password match
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        log.info("Registering new citizen with email: {} and mobile: {}", 
                maskEmail(dto.getEmail()), maskMobile(dto.getMobileNumber()));

        // Check if email already exists
        if (userRepository.existsByEmail(dto.getEmail())) {
            log.warn("Registration failed: Email {} already exists", maskEmail(dto.getEmail()));
            throw new DuplicateUserException("Email already registered");
        }

        // Check if mobile number already exists
        if (userRepository.existsByMobileNumber(dto.getMobileNumber())) {
            log.warn("Registration failed: Mobile number {} already exists", maskMobile(dto.getMobileNumber()));
            throw new DuplicateUserException("Mobile number already registered");
        }

        // Encrypt Aadhar number first (for uniqueness check)
        String encryptedAadhar = encryptionService.encrypt(dto.getAadharNumber().trim());
        
        // Check if Aadhar number already exists (check encrypted value)
        if (userRepository.existsByAadharNumber(encryptedAadhar)) {
            log.warn("Registration failed: Aadhar number already exists");
            throw new DuplicateUserException("Aadhar number already registered");
        }

        // Create new User entity
        User user = new User();
        user.setFirstName(dto.getFirstName().trim());
        user.setLastName(dto.getLastName().trim());
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setMobileNumber(dto.getMobileNumber().trim());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setGender(dto.getGender());
        user.setAddress(dto.getAddress().trim());
        user.setDistrict(dto.getDistrict().trim());
        user.setPincode(dto.getPincode().trim());
        user.setUserType(User.UserType.CITIZEN);
        user.setIsActive(false); // Will be set to true after mobile verification
        user.setIsEmailVerified(false);
        user.setIsMobileVerified(false);

        // Set encrypted Aadhar number
        user.setAadharNumber(encryptedAadhar);

        // Hash password with BCrypt
        String hashedPassword = passwordEncoder.encode(dto.getPassword());
        user.setPassword(hashedPassword);

        // Save user
        User savedUser = userRepository.save(user);
        log.info("Citizen registered successfully with ID: {}", savedUser.getId());

        // Flush to ensure user is persisted before OTP generation
        userRepository.flush();

        // Generate and send DUMMY OTP for mobile verification (logged to console)
        // Allow inactive users for registration flow (allowInactive = true)
        String otpCode = null;
        try {
            log.info("");
            log.info("════════════════════════════════════════════════════════════");
            log.info("GENERATING DUMMY OTP FOR REGISTRATION VERIFICATION");
            log.info("════════════════════════════════════════════════════════════");
            otpCode = otpService.generateOtp(savedUser.getMobileNumber(), User.UserType.CITIZEN, true);
            log.info("════════════════════════════════════════════════════════════");
            log.info("DUMMY OTP GENERATED AND LOGGED TO CONSOLE");
            log.info("════════════════════════════════════════════════════════════");
            log.info("");
        } catch (Exception e) {
            log.error("Failed to send OTP during registration", e);
            // Don't fail registration if OTP sending fails - registration is already successful
        }

        // Return userId and OTP code (temporary - until SMS API is integrated)
        // OTP will be included in API response for testing purposes
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("userId", savedUser.getId());
        result.put("otpCode", otpCode); // May be null if OTP generation failed
        return result;
    }

    /**
     * Find user by email or mobile number
     */
    @Transactional(readOnly = true)
    public User findByEmailOrMobile(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String trimmedUsername = username.trim();
        
        // Check if it's a mobile number (10 digits starting with 6-9)
        if (trimmedUsername.matches("^[6-9]\\d{9}$")) {
            return userRepository.findByMobileNumber(trimmedUsername)
                    .orElseThrow(() -> new RuntimeException("User not found with mobile number: " + trimmedUsername));
        } else {
            // Treat as email
            return userRepository.findByEmail(trimmedUsername.toLowerCase())
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + trimmedUsername));
        }
    }

    /**
     * Verify user credentials
     */
    @Transactional(readOnly = true)
    public User verifyUserCredentials(String username, String password) {
        User user = findByEmailOrMobile(username);
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        return user;
    }

    /**
     * Find user by ID
     */
    @Transactional(readOnly = true)
    public User findById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    /**
     * Verify mobile OTP and activate account
     */
    @Transactional
    public void verifyMobileOtp(String mobileNumber, String otpCode) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User not found with mobile number: " + mobileNumber));

        // Verify OTP
        boolean isValidOtp = otpService.verifyOtp(mobileNumber, otpCode, user.getUserType());
        
        if (!isValidOtp) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Mark OTP as used
        otpService.markOtpAsUsed(mobileNumber, otpCode, user.getUserType());

        // Activate user account
        user.setIsMobileVerified(true);
        user.setIsActive(true);
        userRepository.save(user);
        
        log.info("Mobile number verified and account activated for user ID: {}", user.getId());
    }

    /**
     * Mask email for logging
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 2) {
            return "****@" + parts[1];
        }
        return parts[0].substring(0, 2) + "****@" + parts[1];
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

