package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.PostingAssignmentDTO;
import in.gov.manipur.rccms.dto.PostingDTO;
import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.entity.Court;
import in.gov.manipur.rccms.entity.Officer;
import in.gov.manipur.rccms.entity.OfficerDaHistory;
import in.gov.manipur.rccms.entity.RoleMaster;
import in.gov.manipur.rccms.exception.DuplicateUserException;
import in.gov.manipur.rccms.repository.AdminUnitRepository;
import in.gov.manipur.rccms.repository.CourtRepository;
import in.gov.manipur.rccms.repository.OfficerDaHistoryRepository;
import in.gov.manipur.rccms.repository.OfficerRepository;
import in.gov.manipur.rccms.repository.RoleMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Posting Service
 * Core service for managing postings (assignments of persons to posts)
 * Handles assignment, transfer, and posting history
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostingService {

    private final OfficerDaHistoryRepository postingRepository;
    private final CourtRepository courtRepository;
    private final AdminUnitRepository adminUnitRepository;
    private final RoleMasterRepository roleRepository;
    private final OfficerRepository officerRepository;

    /**
     * Assign a person to a post
     * Supports TWO types of postings:
     * 1. Court-based: Officer posted to a court (courtId provided)
     * 2. Unit-based: Field officer assigned to administrative unit (unitId provided, courtId null)
     * 
     * Business Rules:
     * 1. Either courtId OR unitId must be provided (not both, not neither)
     * 2. Close existing active posting for same COURT+ROLE or UNIT+ROLE
     * 3. Create new posting with is_current = true
     * 4. Generate UserID:
     *    - Court-based: ROLE_CODE@COURT_CODE
     *    - Unit-based: ROLE_CODE@UNIT_LGD_CODE
     * 5. Set from_date = today
     */
    public PostingDTO assignPersonToPost(PostingAssignmentDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Posting assignment data cannot be null");
        }

        // Validate: Either courtId OR unitId must be provided (not both, not neither)
        if (dto.getCourtId() != null && dto.getUnitId() != null) {
            throw new IllegalArgumentException("Cannot provide both courtId and unitId. Provide either courtId (court-based) or unitId (unit-based)");
        }
        if (dto.getCourtId() == null && dto.getUnitId() == null) {
            throw new IllegalArgumentException("Either courtId (court-based posting) or unitId (unit-based posting) must be provided");
        }

        // Validate role exists
        RoleMaster role = roleRepository.findByRoleCode(dto.getRoleCode())
                .orElseThrow(() -> new RuntimeException("Role not found with code: " + dto.getRoleCode()));

        // Validate officer exists
        Officer officer = officerRepository.findById(dto.getOfficerId())
                .orElseThrow(() -> new RuntimeException("Officer not found with ID: " + dto.getOfficerId()));

        Court court = null;
        AdminUnit unit = null;
        String userid;
        boolean isCourtBased = dto.getCourtId() != null;

        if (isCourtBased) {
            // ========== COURT-BASED POSTING ==========
            log.info("Assigning officer to COURT-based post - Court: {}, Role: {}, Officer: {}", 
                    dto.getCourtId(), dto.getRoleCode(), dto.getOfficerId());

            // Validate court exists
            court = courtRepository.findById(dto.getCourtId())
                    .orElseThrow(() -> new RuntimeException("Court not found with ID: " + dto.getCourtId()));

            // Get unit from court
            unit = court.getUnit();
            if (unit == null) {
                throw new RuntimeException("Court does not have an associated unit");
            }

            // Validate court level matches role level (warning only, allow flexibility)
            if (role.getUnitLevel() != null && court.getCourtLevel() != null) {
                String courtLevelName = court.getCourtLevel().name();
                String roleLevelName = role.getUnitLevel().name();
                
                if (!courtLevelName.equals(roleLevelName)) {
                    log.warn("Court level {} does not match role level {} for role {}. Allowing with warning.", 
                            courtLevelName, roleLevelName, role.getRoleCode());
                }
            }

            // Close existing active posting for same COURT + ROLE
            closeExistingActivePostingByCourt(dto.getCourtId(), dto.getRoleCode());

            // Generate UserID: ROLE_CODE@COURT_CODE
            userid = generateCourtBasedUserid(role.getRoleCode(), court.getCourtCode());

        } else {
            // ========== UNIT-BASED POSTING ==========
            log.info("Assigning officer to UNIT-based post - Unit: {}, Role: {}, Officer: {}", 
                    dto.getUnitId(), dto.getRoleCode(), dto.getOfficerId());

            // Validate unit exists
            unit = adminUnitRepository.findById(dto.getUnitId())
                    .orElseThrow(() -> new RuntimeException("Admin unit not found with ID: " + dto.getUnitId()));

            // Validate role level matches unit level (warning only, allow flexibility)
            if (role.getUnitLevel() != null && !role.getUnitLevel().equals(unit.getUnitLevel())) {
                log.warn("Role level {} does not match unit level {} for role {}. Allowing with warning.", 
                        role.getUnitLevel(), unit.getUnitLevel(), role.getRoleCode());
            }

            // Close existing active posting for same UNIT + ROLE
            closeExistingActivePostingByUnit(dto.getUnitId(), dto.getRoleCode());

            // Generate UserID: ROLE_CODE@UNIT_LGD_CODE
            userid = generateUnitBasedUserid(role.getRoleCode(), unit.getLgdCode());
        }

        // Check if UserID already exists (should not happen, but safety check)
        if (postingRepository.findByPostingUserid(userid).isPresent()) {
            log.warn("UserID already exists: {}. This should not happen. Checking for conflicts.", userid);
            postingRepository.findByPostingUseridAndIsCurrentTrue(userid)
                    .ifPresent(existing -> {
                        throw new DuplicateUserException("Active posting with UserID already exists: " + userid);
                    });
        }

        // Create new posting
        OfficerDaHistory posting = new OfficerDaHistory();
        posting.setCourt(court); // NULL for unit-based
        posting.setUnit(unit);
        posting.setRoleCode(role.getRoleCode());
        posting.setOfficer(officer);
        posting.setPostingUserid(userid);
        posting.setFromDate(LocalDate.now());
        posting.setToDate(null); // NULL for current posting
        posting.setIsCurrent(true);

        OfficerDaHistory saved = postingRepository.save(posting);
        log.info("Person assigned to {} post successfully. UserID: {}, Posting ID: {}", 
                isCourtBased ? "COURT-based" : "UNIT-based", saved.getPostingUserid(), saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Transfer a person from one post to another
     * This closes the current posting and creates a new one
     */
    public PostingDTO transferPerson(PostingAssignmentDTO dto) {
        log.info("Transferring person to new post - Court: {}, Role: {}, Officer: {}", 
                dto.getCourtId(), dto.getRoleCode(), dto.getOfficerId());

        // Check if officer has any active postings
        List<OfficerDaHistory> activePostings = postingRepository.findByOfficerIdAndIsCurrentTrue(dto.getOfficerId());
        
        // Close all active postings for this officer
        for (OfficerDaHistory activePosting : activePostings) {
            closePosting(activePosting.getId());
        }

        // Assign to new post
        return assignPersonToPost(dto);
    }

    /**
     * Close existing active posting for court-based posting
     */
    private void closeExistingActivePostingByCourt(Long courtId, String roleCode) {
        List<OfficerDaHistory> activePostings = postingRepository.findActivePostingsByCourtAndRole(courtId, roleCode);
        
        for (OfficerDaHistory posting : activePostings) {
            closePosting(posting.getId());
            log.info("Closed existing active COURT-based posting - Court: {}, Role: {}, Posting ID: {}", 
                    courtId, roleCode, posting.getId());
        }
    }

    /**
     * Close existing active posting for unit-based posting
     */
    private void closeExistingActivePostingByUnit(Long unitId, String roleCode) {
        List<OfficerDaHistory> activePostings = postingRepository.findActivePostingsByUnitAndRole(unitId, roleCode);
        
        for (OfficerDaHistory posting : activePostings) {
            closePosting(posting.getId());
            log.info("Closed existing active UNIT-based posting - Unit: {}, Role: {}, Posting ID: {}", 
                    unitId, roleCode, posting.getId());
        }
    }

    /**
     * Close a posting by ID
     */
    public void closePosting(Long postingId) {
        OfficerDaHistory posting = postingRepository.findById(postingId)
                .orElseThrow(() -> new RuntimeException("Posting not found with ID: " + postingId));

        if (posting.getIsCurrent()) {
            posting.setToDate(LocalDate.now());
            posting.setIsCurrent(false);
            postingRepository.save(posting);
            log.info("Posting closed - ID: {}, UserID: {}", postingId, posting.getPostingUserid());
        }
    }

    /**
     * Generate UserID for court-based posting: ROLE_CODE@COURT_CODE
     */
    private String generateCourtBasedUserid(String roleCode, String courtCode) {
        return roleCode + "@" + courtCode;
    }

    /**
     * Generate UserID for unit-based posting: ROLE_CODE@UNIT_LGD_CODE
     */
    private String generateUnitBasedUserid(String roleCode, Long unitLgdCode) {
        return roleCode + "@" + unitLgdCode;
    }

    /**
     * Get active posting by UserID
     */
    @Transactional(readOnly = true)
    public PostingDTO getActivePostingByUserid(String userid) {
        OfficerDaHistory posting = postingRepository.findByPostingUseridAndIsCurrentTrue(userid)
                .orElseThrow(() -> new RuntimeException("Active posting not found with UserID: " + userid));

        return convertToDTO(posting);
    }

    /**
     * Get all postings by officer (person)
     */
    @Transactional(readOnly = true)
    public List<PostingDTO> getPostingsByOfficer(Long officerId) {
        List<OfficerDaHistory> postings = postingRepository.findByOfficerIdOrderByFromDateDesc(officerId);
        return postings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all postings by court
     */
    @Transactional(readOnly = true)
    public List<PostingDTO> getPostingsByCourt(Long courtId) {
        List<OfficerDaHistory> postings = postingRepository.findByCourtIdOrderByFromDateDesc(courtId);
        return postings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all active postings by unit
     * Returns both court-based postings (courts in this unit) and unit-based postings (directly to this unit)
     */
    @Transactional(readOnly = true)
    public List<PostingDTO> getActivePostingsByUnit(Long unitId) {
        // Get court-based postings (courts in this unit)
        List<OfficerDaHistory> courtBasedPostings = postingRepository.findActivePostingsByUnit(unitId);
        
        // Get unit-based postings (directly to this unit)
        List<OfficerDaHistory> unitBasedPostings = postingRepository.findByUnitIdAndIsCurrentTrue(unitId);
        
        // Combine and convert
        List<OfficerDaHistory> allPostings = new java.util.ArrayList<>();
        allPostings.addAll(courtBasedPostings);
        allPostings.addAll(unitBasedPostings);
        
        return allPostings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get field officers (unit-based postings) available to a court
     * Searches unit hierarchy: finds unit-based officers in units under the court's unit
     */
    @Transactional(readOnly = true)
    public List<PostingDTO> getFieldOfficersForCourt(Long courtId, String roleCode) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new RuntimeException("Court not found with ID: " + courtId));
        
        if (court.getUnit() == null) {
            throw new RuntimeException("Court does not have an associated unit");
        }
        
        // Find unit-based officers in the unit hierarchy
        List<OfficerDaHistory> fieldOfficers = postingRepository.findFieldOfficersForCourt(
                court.getUnit().getUnitId(), roleCode);
        
        return fieldOfficers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all unit-based postings (field officers) by role
     */
    @Transactional(readOnly = true)
    public List<PostingDTO> getUnitBasedPostingsByRole(String roleCode) {
        List<OfficerDaHistory> postings = postingRepository.findUnitBasedPostingsByRole(roleCode);
        return postings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all unit-based postings (field officers) by unit and role
     */
    @Transactional(readOnly = true)
    public List<PostingDTO> getUnitBasedPostingsByUnitAndRole(Long unitId, String roleCode) {
        List<OfficerDaHistory> postings = postingRepository.findActivePostingsByUnitAndRole(unitId, roleCode);
        return postings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all field officers (unit-based postings) below a unit in hierarchy
     * Returns all unit-based officers in units under the given unit (all roles)
     * Useful for showing all field officers available to a Tehsildar
     */
    @Transactional(readOnly = true)
    public List<PostingDTO> getAllFieldOfficersBelowUnit(Long unitId) {
        List<OfficerDaHistory> fieldOfficers = postingRepository.findAllFieldOfficersBelowUnit(unitId);
        return fieldOfficers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all active postings
     */
    @Transactional(readOnly = true)
    public List<PostingDTO> getAllActivePostings() {
        List<OfficerDaHistory> postings = postingRepository.findByIsCurrentTrueOrderByFromDateDesc();
        return postings.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert Entity to DTO
     * Handles both court-based and unit-based postings
     */
    private PostingDTO convertToDTO(OfficerDaHistory posting) {
        PostingDTO dto = new PostingDTO();
        dto.setId(posting.getId());
        
        // Court information (for court-based postings)
        dto.setCourtId(posting.getCourtId());
        if (posting.getCourt() != null) {
            dto.setCourtName(posting.getCourt().getCourtName());
            dto.setCourtCode(posting.getCourt().getCourtCode());
            dto.setCourtLevel(posting.getCourt().getCourtLevel().name());
            dto.setCourtType(posting.getCourt().getCourtType().name());
            
            // Derive unit information from court
            if (posting.getCourt().getUnit() != null) {
                dto.setUnitId(posting.getCourt().getUnit().getUnitId());
                dto.setUnitName(posting.getCourt().getUnit().getUnitName());
                dto.setUnitCode(posting.getCourt().getUnit().getUnitCode());
                dto.setUnitLgdCode(posting.getCourt().getUnit().getLgdCode().toString());
            }
        }
        
        // Unit information (for unit-based postings or as direct reference)
        if (posting.getUnit() != null) {
            // If unit is directly set (unit-based posting), use it
            // If unit is null but court exists, unit info was already set from court above
            if (posting.getCourtId() == null) {
                // This is a unit-based posting
                dto.setUnitId(posting.getUnit().getUnitId());
                dto.setUnitName(posting.getUnit().getUnitName());
                dto.setUnitCode(posting.getUnit().getUnitCode());
                dto.setUnitLgdCode(posting.getUnit().getLgdCode().toString());
            }
        }
        
        dto.setRoleCode(posting.getRoleCode());
        dto.setOfficerId(posting.getOfficerId());
        if (posting.getOfficer() != null) {
            dto.setOfficerName(posting.getOfficer().getFullName());
            dto.setMobileNo(posting.getOfficer().getMobileNo());
        }
        dto.setPostingUserid(posting.getPostingUserid());
        dto.setFromDate(posting.getFromDate());
        dto.setToDate(posting.getToDate());
        dto.setIsCurrent(posting.getIsCurrent());
        
        // Add posting type indicator
        dto.setPostingType(posting.getCourtId() != null ? "COURT_BASED" : "UNIT_BASED");
        
        return dto;
    }
}

