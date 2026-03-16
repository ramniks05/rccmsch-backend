package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.CitizenRegistrationDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.entity.Citizen;
import in.gov.manipur.rccms.entity.Lawyer;
import in.gov.manipur.rccms.entity.RegistrationFormField;
import in.gov.manipur.rccms.entity.RoleMaster;
import in.gov.manipur.rccms.exception.DuplicateUserException;
import in.gov.manipur.rccms.repository.LawyerRepository;
import in.gov.manipur.rccms.repository.RoleMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lawyer Service
 * Handles lawyer registration and management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LawyerService {

    private final LawyerRepository lawyerRepository;
    private final RoleMasterRepository roleMasterRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final RegistrationFormService registrationFormService;
    private final ObjectMapper objectMapper;

    /**
     * Register a new lawyer
     */
    public java.util.Map<String, Object> registerLawyer(CitizenRegistrationDTO dto) {
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
            registrationFormService.validateRegistrationData(RegistrationFormField.RegistrationType.LAWYER, dataMap);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("Registration schema validation failed");
        }

        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password do not match");
        }

        if (lawyerRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateUserException("Email already registered");
        }
        if (lawyerRepository.existsByMobileNumber(dto.getMobileNumber())) {
            throw new DuplicateUserException("Mobile number already registered");
        }

        RoleMaster lawyerRole = roleMasterRepository.findByRoleCode("LAWYER")
                .orElseThrow(() -> new IllegalStateException("Role not found in role_master: LAWYER. Ensure RoleDataInitializer has seeded LAWYER."));

        Lawyer lawyer = new Lawyer();
        lawyer.setRole(lawyerRole);
        lawyer.setFirstName(dto.getFirstName().trim());
        lawyer.setLastName(dto.getLastName().trim());
        lawyer.setEmail(dto.getEmail().trim().toLowerCase());
        lawyer.setMobileNumber(dto.getMobileNumber().trim());

        // Store dynamic registration data (JSON)
        if (dto.getExtraFields() != null && !dto.getExtraFields().isEmpty()) {
            try {
                lawyer.setRegistrationData(objectMapper.writeValueAsString(dto.getExtraFields()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid registration data format");
            }
        }
        lawyer.setPassword(passwordEncoder.encode(dto.getPassword()));
        lawyer.setIsActive(false);
        lawyer.setIsEmailVerified(false);
        lawyer.setIsMobileVerified(false);

        Lawyer saved = lawyerRepository.save(lawyer);
        lawyerRepository.flush();

        String otpCode = null;
        try {
            otpCode = otpService.generateOtp(saved.getMobileNumber(), Citizen.CitizenType.LAWYER, true);
        } catch (Exception e) {
            log.error("Failed to send OTP during lawyer registration", e);
        }

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("citizenId", saved.getId());
        result.put("otpCode", otpCode);
        return result;
    }

    /**
     * Find lawyer by email or mobile
     */
    @Transactional(readOnly = true)
    public Lawyer findByEmailOrMobile(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        String trimmed = username.trim();
        if (trimmed.matches("^[6-9]\\d{9}$")) {
            return lawyerRepository.findByMobileNumber(trimmed)
                    .orElseThrow(() -> new RuntimeException("Lawyer not found with mobile number: " + trimmed));
        }
        return lawyerRepository.findByEmail(trimmed.toLowerCase())
                .orElseThrow(() -> new RuntimeException("Lawyer not found with email: " + trimmed));
    }

    /**
     * Verify lawyer credentials
     */
    @Transactional(readOnly = true)
    public Lawyer verifyLawyerCredentials(String username, String password) {
        Lawyer lawyer = findByEmailOrMobile(username);
        if (!passwordEncoder.matches(password, lawyer.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        return lawyer;
    }

    /**
     * Find lawyer by ID
     */
    @Transactional(readOnly = true)
    public Lawyer findById(Long lawyerId) {
        if (lawyerId == null) {
            throw new IllegalArgumentException("Lawyer ID cannot be null");
        }
        return lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new RuntimeException("Lawyer not found with ID: " + lawyerId));
    }

    /**
     * Verify mobile OTP and activate account
     */
    @Transactional
    public void verifyMobileOtp(String mobileNumber, String otpCode) {
        Lawyer lawyer = lawyerRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("Lawyer not found with mobile number: " + mobileNumber));

        boolean isValidOtp = otpService.verifyOtp(mobileNumber, otpCode, Citizen.CitizenType.LAWYER);
        if (!isValidOtp) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        otpService.markOtpAsUsed(mobileNumber, otpCode, Citizen.CitizenType.LAWYER);
        lawyer.setIsMobileVerified(true);
        lawyer.setIsActive(true);
        lawyerRepository.save(lawyer);
    }
}
