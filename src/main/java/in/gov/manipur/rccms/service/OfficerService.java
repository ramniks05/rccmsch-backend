package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.OfficerDTO;
import in.gov.manipur.rccms.entity.Officer;
import in.gov.manipur.rccms.exception.DuplicateUserException;
import in.gov.manipur.rccms.repository.OfficerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Officer Service
 * Handles CRUD operations for Officers (Government Employees)
 * Includes password generation logic for temporary passwords
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OfficerService {

    private final OfficerRepository officerRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new officer (government employee)
     * Generates temporary password: Rccms@<last4MobileDigits>
     */
    public OfficerDTO createOfficer(OfficerDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Officer data cannot be null");
        }

        log.info("Creating officer (government employee) with mobile: {}", maskMobile(dto.getMobileNo()));

        // Check if mobile number already exists
        if (officerRepository.existsByMobileNo(dto.getMobileNo())) {
            log.warn("Officer creation failed: Mobile number {} already exists", maskMobile(dto.getMobileNo()));
            throw new DuplicateUserException("Mobile number already registered");
        }

        // Check if email already exists (if provided)
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            if (officerRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
                log.warn("Officer creation failed: Email {} already exists", dto.getEmail());
                throw new DuplicateUserException("Email already registered");
            }
        }

        // Create entity
        Officer officer = new Officer();
        officer.setFullName(dto.getFullName().trim());
        officer.setMobileNo(dto.getMobileNo().trim());
        officer.setEmail(dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : null);
        officer.setAuthType(Officer.AuthType.TEMP_PASSWORD);
        officer.setIsActive(true);
        officer.setIsPasswordResetRequired(true);
        officer.setIsMobileVerified(false);

        // Generate temporary password: Rccms@<last4MobileDigits>
        String tempPassword = generateTemporaryPassword(dto.getMobileNo());
        String passwordHash = passwordEncoder.encode(tempPassword);
        officer.setPasswordHash(passwordHash);

        Officer saved = officerRepository.save(officer);
        log.info("Officer created successfully with ID: {}. Temporary password generated.", saved.getId());

        // Log temporary password (in production, send via secure channel)
        log.info("========================================");
        log.info("TEMPORARY PASSWORD FOR OFFICER: {}", saved.getId());
        log.info("Mobile: {}", dto.getMobileNo());
        log.info("Temporary Password: {}", tempPassword);
        log.info("========================================");

        return convertToDTO(saved);
    }

    /**
     * Generate temporary password: Rccms@<last4MobileDigits>
     */
    public String generateTemporaryPassword(String mobileNo) {
        if (mobileNo == null || mobileNo.length() < 4) {
            throw new IllegalArgumentException("Mobile number must be at least 4 digits");
        }
        String last4Digits = mobileNo.substring(mobileNo.length() - 4);
        return "Rccms@" + last4Digits;
    }

    /**
     * Get officer by ID
     */
    @Transactional(readOnly = true)
    public OfficerDTO getOfficerById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Officer ID cannot be null");
        }

        Officer officer = officerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Officer not found with ID: " + id));

        return convertToDTO(officer);
    }

    /**
     * Get all officers
     */
    @Transactional(readOnly = true)
    public List<OfficerDTO> getAllOfficers() {
        List<Officer> officers = officerRepository.findAllByOrderByFullNameAsc();
        return officers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update officer
     */
    public OfficerDTO updateOfficer(Long id, OfficerDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("Officer ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Officer data cannot be null");
        }

        log.info("Updating officer with ID: {}", id);

        Officer officer = officerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Officer not found with ID: " + id));

        // Update fields
        if (dto.getFullName() != null) {
            officer.setFullName(dto.getFullName().trim());
        }
        if (dto.getEmail() != null) {
            // Check if email is being changed and if new email already exists
            if (!officer.getEmail().equals(dto.getEmail().trim().toLowerCase())) {
                if (officerRepository.existsByEmail(dto.getEmail().trim().toLowerCase())) {
                    throw new DuplicateUserException("Email already registered");
                }
                officer.setEmail(dto.getEmail().trim().toLowerCase());
            }
        }
        if (dto.getIsActive() != null) {
            officer.setIsActive(dto.getIsActive());
        }
        if (dto.getIsMobileVerified() != null) {
            officer.setIsMobileVerified(dto.getIsMobileVerified());
        }

        Officer updated = officerRepository.save(officer);
        log.info("Officer updated successfully with ID: {}", updated.getId());

        return convertToDTO(updated);
    }

    /**
     * Reset password (for first login)
     */
    public void resetPassword(Long officerId, String newPassword) {
        Officer officer = officerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Officer not found with ID: " + officerId));

        String passwordHash = passwordEncoder.encode(newPassword);
        officer.setPasswordHash(passwordHash);
        officer.setIsPasswordResetRequired(false);
        officerRepository.save(officer);

        log.info("Password reset successfully for officer ID: {}", officerId);
    }

    /**
     * Verify mobile number
     */
    public void verifyMobile(Long officerId) {
        Officer officer = officerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Officer not found with ID: " + officerId));

        officer.setIsMobileVerified(true);
        officerRepository.save(officer);

        log.info("Mobile number verified for officer ID: {}", officerId);
    }

    /**
     * Convert Entity to DTO
     */
    private OfficerDTO convertToDTO(Officer officer) {
        OfficerDTO dto = new OfficerDTO();
        dto.setId(officer.getId());
        dto.setFullName(officer.getFullName());
        dto.setMobileNo(officer.getMobileNo());
        dto.setEmail(officer.getEmail());
        dto.setAuthType(officer.getAuthType());
        dto.setIsActive(officer.getIsActive());
        dto.setIsPasswordResetRequired(officer.getIsPasswordResetRequired());
        dto.setIsMobileVerified(officer.getIsMobileVerified());
        return dto;
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
}

