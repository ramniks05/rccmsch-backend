package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.CitizenProfileUpdateDTO;
import in.gov.manipur.rccms.dto.CitizenRegistrationDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.entity.Citizen;
import in.gov.manipur.rccms.entity.RegistrationFormField;
import in.gov.manipur.rccms.exception.DuplicateUserException;
import in.gov.manipur.rccms.repository.CitizenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Citizen Service
 * Handles citizen registration and management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CitizenService {

    private final CitizenRepository citizenRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final RegistrationFormService registrationFormService;
    private final ObjectMapper objectMapper;

    /**
     * Register a new citizen
     * @param dto registration DTO
     * @return Map containing citizenId and otpCode (temporary - until SMS API is integrated)
     * @throws DuplicateUserException if email/mobile/aadhar already exists
     */
    public java.util.Map<String, Object> registerCitizen(CitizenRegistrationDTO dto) {
        return registerCitizenWithType(dto, Citizen.CitizenType.CITIZEN);
    }

    public CitizenProfileUpdateDTO updateCitizenProfile(Long citizenId, CitizenProfileUpdateDTO dto) {

        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new RuntimeException("Citizen not found"));

        citizen.setFirstName(dto.getFirstName());
        citizen.setLastName(dto.getLastName());
        citizen.setEmail(dto.getEmail());

        if (dto.getExtraFields() != null && !dto.getExtraFields().isEmpty()) {
            try {
                citizen.setRegistrationData(objectMapper.writeValueAsString(dto.getExtraFields()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid registration data format");
            }
        }


        Citizen savedData = citizenRepository.save(citizen);
        return new CitizenProfileUpdateDTO(savedData,objectMapper);
    }

    /**
     * Register citizen with specific type
     */
    public java.util.Map<String, Object> registerCitizenWithType(CitizenRegistrationDTO dto, Citizen.CitizenType citizenType) {
        if (dto == null) {
            throw new IllegalArgumentException("Registration data cannot be null");
        }

        // Validate against dynamic registration schema (if configured)
        try {
            java.util.Map<String, Object> dataMap = objectMapper.convertValue(dto, new TypeReference<java.util.Map<String, Object>>() {});
            if (dto.getExtraFields() != null && !dto.getExtraFields().isEmpty()) {
                dataMap.putAll(dto.getExtraFields());
            }
            dataMap.remove("extraFields");
            registrationFormService.validateRegistrationData(RegistrationFormField.RegistrationType.CITIZEN, dataMap);
        } catch (Exception e) {
            // Keep original error message from validation
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("Registration schema validation failed");
        }

        // Validate password match
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        log.info("Registering new citizen with email: {} and mobile: {}", 
                maskEmail(dto.getEmail()), maskMobile(dto.getMobileNumber()));

        // Check if email already exists
        if (citizenRepository.existsByEmail(dto.getEmail())) {
            log.warn("Registration failed: Email {} already exists", maskEmail(dto.getEmail()));
            throw new DuplicateUserException("Email already registered");
        }

        // Check if mobile number already exists
        if (citizenRepository.existsByMobileNumber(dto.getMobileNumber())) {
            log.warn("Registration failed: Mobile number {} already exists", maskMobile(dto.getMobileNumber()));
            throw new DuplicateUserException("Mobile number already registered");
        }

        // Create new Citizen entity
        Citizen citizen = new Citizen();
        citizen.setFirstName(dto.getFirstName().trim());
        citizen.setLastName(dto.getLastName().trim());
        citizen.setEmail(dto.getEmail().trim().toLowerCase());
        citizen.setMobileNumber(dto.getMobileNumber().trim());
        citizen.setCitizenType(citizenType);
        citizen.setIsActive(false); // Will be set to true after mobile verification
        citizen.setIsEmailVerified(false);
        citizen.setIsMobileVerified(false);

        // Store dynamic registration data (JSON)
        if (dto.getExtraFields() != null && !dto.getExtraFields().isEmpty()) {
            try {
                citizen.setRegistrationData(objectMapper.writeValueAsString(dto.getExtraFields()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid registration data format");
            }
        }

        // Hash password with BCrypt
        String hashedPassword = passwordEncoder.encode(dto.getPassword());
        citizen.setPassword(hashedPassword);

        // Save citizen
        Citizen savedCitizen = citizenRepository.save(citizen);
        log.info("Citizen registered successfully with ID: {}", savedCitizen.getId());

        // Flush to ensure citizen is persisted before OTP generation
        citizenRepository.flush();

        // Generate and send DUMMY OTP for mobile verification (logged to console)
        // Allow inactive citizens for registration flow (allowInactive = true)
        String otpCode = null;
        try {
            log.info("");
            log.info("════════════════════════════════════════════════════════════");
            log.info("GENERATING DUMMY OTP FOR REGISTRATION VERIFICATION");
            log.info("════════════════════════════════════════════════════════════");
            otpCode = otpService.generateOtp(savedCitizen.getMobileNumber(), citizenType, true);
            log.info("════════════════════════════════════════════════════════════");
            log.info("DUMMY OTP GENERATED AND LOGGED TO CONSOLE");
            log.info("════════════════════════════════════════════════════════════");
            log.info("");
        } catch (Exception e) {
            log.error("Failed to send OTP during registration", e);
            // Don't fail registration if OTP sending fails - registration is already successful
        }

        // Return citizenId and OTP code (temporary - until SMS API is integrated)
        // OTP will be included in API response for testing purposes
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("citizenId", savedCitizen.getId());
        result.put("otpCode", otpCode); // May be null if OTP generation failed
        return result;
    }

    /**
     * Find citizen by email or mobile number
     */
    @Transactional(readOnly = true)
    public Citizen findByEmailOrMobile(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String trimmedUsername = username.trim();
        
        // Check if it's a mobile number (10 digits starting with 6-9)
        if (trimmedUsername.matches("^[6-9]\\d{9}$")) {
            return citizenRepository.findByMobileNumber(trimmedUsername)
                    .orElseThrow(() -> new RuntimeException("Citizen not found with mobile number: " + trimmedUsername));
        } else {
            // Treat as email
            return citizenRepository.findByEmail(trimmedUsername.toLowerCase())
                    .orElseThrow(() -> new RuntimeException("Citizen not found with email: " + trimmedUsername));
        }
    }

    /**
     * Verify citizen credentials
     */
    @Transactional(readOnly = true)
    public Citizen verifyCitizenCredentials(String username, String password) {
        Citizen citizen = findByEmailOrMobile(username);
        
        if (!passwordEncoder.matches(password, citizen.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        
        return citizen;
    }

    /**
     * Find citizen by ID
     */
    @Transactional(readOnly = true)
    public Citizen findById(Long citizenId) {
        if (citizenId == null) {
            throw new IllegalArgumentException("Citizen ID cannot be null");
        }
        return citizenRepository.findById(citizenId)
                .orElseThrow(() -> new RuntimeException("Citizen not found with ID: " + citizenId));
    }

    /**
     * Verify mobile OTP and activate account
     */
    @Transactional
    public void verifyMobileOtp(String mobileNumber, String otpCode) {
        Citizen citizen = citizenRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("Citizen not found with mobile number: " + mobileNumber));

        // Verify OTP
        boolean isValidOtp = otpService.verifyOtp(mobileNumber, otpCode, citizen.getCitizenType());
        
        if (!isValidOtp) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Mark OTP as used
        otpService.markOtpAsUsed(mobileNumber, otpCode, citizen.getCitizenType());

        // Activate citizen account
        citizen.setIsMobileVerified(true);
        citizen.setIsActive(true);
        citizenRepository.save(citizen);
        
        log.info("Mobile number verified and account activated for citizen ID: {}", citizen.getId());
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

