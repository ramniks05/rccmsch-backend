package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.PostingAssignmentDTO;
import in.gov.manipur.rccms.dto.PostingDTO;
import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.entity.Court;
import in.gov.manipur.rccms.entity.Officer;
import in.gov.manipur.rccms.entity.OfficerDaHistory;
import in.gov.manipur.rccms.entity.RoleMaster;
import in.gov.manipur.rccms.exception.DuplicateUserException;
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
    private final RoleMasterRepository roleRepository;
    private final OfficerRepository officerRepository;

    /**
     * Assign a person to a post (COURT + ROLE)
     * This is the core business logic for posting assignments
     * 
     * Business Rules:
     * 1. Close existing active posting for same COURT + ROLE
     * 2. Create new posting with is_current = true
     * 3. Generate UserID: ROLE_CODE@COURT_CODE
     * 4. Set from_date = today
     */
    public PostingDTO assignPersonToPost(PostingAssignmentDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Posting assignment data cannot be null");
        }

        log.info("Assigning officer to post - Court: {}, Role: {}, Officer: {}", 
                dto.getCourtId(), dto.getRoleCode(), dto.getOfficerId());

        // Validate court exists
        Court court = courtRepository.findById(dto.getCourtId())
                .orElseThrow(() -> new RuntimeException("Court not found with ID: " + dto.getCourtId()));

        // Validate role exists
        RoleMaster role = roleRepository.findByRoleCode(dto.getRoleCode())
                .orElseThrow(() -> new RuntimeException("Role not found with code: " + dto.getRoleCode()));

        // Validate officer exists
        Officer officer = officerRepository.findById(dto.getOfficerId())
                .orElseThrow(() -> new RuntimeException("Officer not found with ID: " + dto.getOfficerId()));

        // Validate court level matches role level (except for DEALING_ASSISTANT)
        if (role.getUnitLevel() != null && court.getCourtLevel() != null) {
            // Map CourtLevel to AdminUnit.UnitLevel for comparison
            AdminUnit.UnitLevel roleUnitLevel = role.getUnitLevel();
            String courtLevelName = court.getCourtLevel().name();
            String roleLevelName = roleUnitLevel.name();
            
            if (!courtLevelName.equals(roleLevelName)) {
                log.warn("Court level {} does not match role level {} for role {}", 
                        courtLevelName, roleLevelName, role.getRoleCode());
                // Allow but log warning - some flexibility
            }
        }

        // Step 1: Close existing active posting for same COURT + ROLE
        closeExistingActivePosting(dto.getCourtId(), dto.getRoleCode());

        // Step 2: Generate UserID: ROLE_CODE@COURT_CODE
        String userid = generateUserid(role.getRoleCode(), court.getCourtCode());

        // Step 3: Check if UserID already exists (should not happen, but safety check)
        if (postingRepository.findByPostingUserid(userid).isPresent()) {
            log.warn("UserID already exists: {}. This should not happen. Checking for conflicts.", userid);
            // If exists but not current, it's okay (old posting)
            postingRepository.findByPostingUseridAndIsCurrentTrue(userid)
                    .ifPresent(existing -> {
                        throw new DuplicateUserException("Active posting with UserID already exists: " + userid);
                    });
        }

        // Step 4: Create new posting
        OfficerDaHistory posting = new OfficerDaHistory();
        posting.setCourt(court);
        posting.setRoleCode(role.getRoleCode());
        posting.setOfficer(officer);
        posting.setPostingUserid(userid);
        posting.setFromDate(LocalDate.now());
        posting.setToDate(null); // NULL for current posting
        posting.setIsCurrent(true);

        OfficerDaHistory saved = postingRepository.save(posting);
        log.info("Person assigned to post successfully. UserID: {}, Posting ID: {}", saved.getPostingUserid(), saved.getId());

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
     * Close an existing active posting
     */
    private void closeExistingActivePosting(Long courtId, String roleCode) {
        List<OfficerDaHistory> activePostings = postingRepository.findActivePostingsByCourtAndRole(courtId, roleCode);
        
        for (OfficerDaHistory posting : activePostings) {
            closePosting(posting.getId());
            log.info("Closed existing active posting - Court: {}, Role: {}, Posting ID: {}", 
                    courtId, roleCode, posting.getId());
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
     * Generate UserID: ROLE_CODE@COURT_CODE
     */
    private String generateUserid(String roleCode, String courtCode) {
        return roleCode + "@" + courtCode;
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
     * Get all active postings by unit (through court)
     */
    @Transactional(readOnly = true)
    public List<PostingDTO> getActivePostingsByUnit(Long unitId) {
        List<OfficerDaHistory> postings = postingRepository.findActivePostingsByUnit(unitId);
        return postings.stream()
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
     */
    private PostingDTO convertToDTO(OfficerDaHistory posting) {
        PostingDTO dto = new PostingDTO();
        dto.setId(posting.getId());
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
        return dto;
    }
}

