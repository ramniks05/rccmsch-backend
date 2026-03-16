package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.AuthResponseDTO;
import in.gov.manipur.rccms.dto.PostingDetailsDTO;
import in.gov.manipur.rccms.dto.PostingHierarchyDTO;
import in.gov.manipur.rccms.dto.PostBasedLoginDTO;
import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.entity.Court;
import in.gov.manipur.rccms.entity.Officer;
import in.gov.manipur.rccms.entity.OfficerDaHistory;
import in.gov.manipur.rccms.entity.RoleMaster;
import in.gov.manipur.rccms.exception.InvalidCredentialsException;
import in.gov.manipur.rccms.repository.AdminUnitRepository;
import in.gov.manipur.rccms.repository.OfficerDaHistoryRepository;
import in.gov.manipur.rccms.repository.OfficerRepository;
import in.gov.manipur.rccms.repository.RoleMasterRepository;
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
    private final AdminUnitRepository adminUnitRepository;
    private final RoleMasterRepository roleMasterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Login with UserID and password (post-based login)
     * UserID format: ROLE_CODE@COURT_CODE
     */
    @Transactional
    public AuthResponseDTO loginWithPostBasedCredentials(PostBasedLoginDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }

        log.info("Post-based login attempt for UserID: {}", request.getUserid());

        // Find active posting by UserID - eagerly fetches court, unit, and officer
        OfficerDaHistory posting = postingRepository.findByPostingUseridAndIsCurrentTrue(request.getUserid())
                .orElseThrow(() -> {
                    log.warn("Login failed: Active posting not found for UserID: {}", request.getUserid());
                    return new InvalidCredentialsException("Invalid UserID or posting is not active");
                });

        // Determine posting type and get unit
        Court court = posting.getCourt();
        AdminUnit unit;
        boolean isCourtBased = (court != null);

        if (isCourtBased && court != null) {
            // Court-based posting: get unit from court
            if (court.getUnit() == null) {
                throw new RuntimeException("Unit not found for court in posting UserID: " + request.getUserid());
            }
            unit = court.getUnit();
        } else {
            // Unit-based posting: get unit directly
            unit = posting.getUnit();
            if (unit == null) {
                throw new RuntimeException("Unit not found for unit-based posting UserID: " + request.getUserid());
            }
        }
        
        // Role from posting (role_id is FK to role_master)
        RoleMaster role = posting.getRole();
        if (role == null) {
            role = roleMasterRepository.findByRoleCode(posting.getRoleCode())
                    .orElseThrow(() -> new RuntimeException("Role not found for role code: " + posting.getRoleCode()));
        }

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

        // Generate JWT token with posting information (include roleId for workflow)
        Long roleId = role.getId();
        String accessToken = jwtService.generatePostBasedToken(
                posting.getPostingUserid(),
                posting.getRoleCode(),
                roleId,
                unit.getUnitId(),
                unit.getUnitLevel().name(),
                officer.getId()
        );
        
        String refreshToken = jwtService.generateRefreshToken(officer.getId(), officer.getEmail());

        // Build hierarchy chain (State → District → Sub-Division → Circle)
        PostingHierarchyDTO hierarchy = buildHierarchy(unit);

        // Build posting details
        PostingDetailsDTO.PostingDetailsDTOBuilder postingBuilder = PostingDetailsDTO.builder()
                .postingId(posting.getId())
                .postingUserid(posting.getPostingUserid())
                .fromDate(posting.getFromDate())
                .toDate(posting.getToDate())
                .isCurrent(posting.getIsCurrent())
                .roleCode(role.getRoleCode())
                .roleName(role.getRoleName())
                .unitId(unit.getUnitId())
                .unitCode(unit.getUnitCode())
                .unitName(unit.getUnitName())
                .unitLgdCode(unit.getLgdCode())
                .hierarchy(hierarchy)
                .officerId(officer.getId())
                .officerName(officer.getFullName())
                .officerEmail(officer.getEmail())
                .officerMobile(officer.getMobileNo());

        // Add court information only for court-based postings
        if (isCourtBased && court != null) {
            postingBuilder.courtId(court.getId())
                    .courtCode(court.getCourtCode())
                    .courtName(court.getCourtName())
                    .courtLevel(court.getCourtLevel().name())
                    .courtType(court.getCourtType().name());
        }

        PostingDetailsDTO postingDetails = postingBuilder.build();

        log.info("Post-based login successful for UserID: {}, Officer: {}", request.getUserid(), officer.getFullName());

        return AuthResponseDTO.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(officer.getId())
                .citizenType(null) // Not applicable for post-based login
                .email(officer.getEmail())
                .mobileNumber(officer.getMobileNo())
                .expiresIn(3600) // 1 hour in seconds
                .posting(postingDetails)
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

    /**
     * Build hierarchy chain from unit (State → District → Sub-Division → Circle)
     * Traverses up the parent chain to build complete hierarchy
     */
    private PostingHierarchyDTO buildHierarchy(AdminUnit unit) {
        if (unit == null) {
            return null;
        }

        PostingHierarchyDTO.PostingHierarchyDTOBuilder builder = PostingHierarchyDTO.builder()
                .unitId(unit.getUnitId())
                .unitCode(unit.getUnitCode())
                .unitName(unit.getUnitName())
                .unitLevel(unit.getUnitLevel())
                .lgdCode(unit.getLgdCode());

        // Traverse up the parent chain to build hierarchy
        AdminUnit current = unit;
        PostingHierarchyDTO circle = null;
        PostingHierarchyDTO subDivision = null;
        PostingHierarchyDTO district = null;
        PostingHierarchyDTO state = null;

        // Start from current unit and traverse up
        while (current != null) {
            PostingHierarchyDTO hierarchyItem = PostingHierarchyDTO.builder()
                    .unitId(current.getUnitId())
                    .unitCode(current.getUnitCode())
                    .unitName(current.getUnitName())
                    .unitLevel(current.getUnitLevel())
                    .lgdCode(current.getLgdCode())
                    .build();

            // Assign based on level
            switch (current.getUnitLevel()) {
                case CIRCLE:
                    circle = hierarchyItem;
                    break;
                case SUB_DIVISION:
                    subDivision = hierarchyItem;
                    break;
                case DISTRICT:
                    district = hierarchyItem;
                    break;
                case STATE:
                    state = hierarchyItem;
                    break;
            }

            // Move to parent
            Long parentUnitId = current.getParentUnitId();
            if (parentUnitId != null) {
                current = adminUnitRepository.findById(parentUnitId).orElse(null);
            } else {
                current = null;
            }
        }

        // Set hierarchy in builder
        builder.circle(circle)
                .subDivision(subDivision)
                .district(district)
                .state(state);

        return builder.build();
    }
}

