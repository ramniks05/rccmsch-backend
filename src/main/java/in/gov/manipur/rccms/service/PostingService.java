package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.PostingAssignmentDTO;
import in.gov.manipur.rccms.dto.PostingDTO;
import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.entity.Officer;
import in.gov.manipur.rccms.entity.OfficerDaHistory;
import in.gov.manipur.rccms.entity.RoleMaster;
import in.gov.manipur.rccms.exception.DuplicateUserException;
import in.gov.manipur.rccms.repository.AdminUnitRepository;
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
    private final AdminUnitRepository unitRepository;
    private final RoleMasterRepository roleRepository;
    private final OfficerRepository officerRepository;

    /**
     * Assign a person to a post (UNIT + ROLE)
     * This is the core business logic for posting assignments
     * 
     * Business Rules:
     * 1. Close existing active posting for same UNIT + ROLE
     * 2. Create new posting with is_current = true
     * 3. Generate UserID: ROLE_CODE@UNIT_LGD_CODE
     * 4. Set from_date = today
     */
    public PostingDTO assignPersonToPost(PostingAssignmentDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Posting assignment data cannot be null");
        }

        log.info("Assigning officer to post - Unit: {}, Role: {}, Officer: {}", 
                dto.getUnitId(), dto.getRoleCode(), dto.getOfficerId());

        // Validate unit exists
        AdminUnit unit = unitRepository.findById(dto.getUnitId())
                .orElseThrow(() -> new RuntimeException("Unit not found with ID: " + dto.getUnitId()));

        // Validate role exists
        RoleMaster role = roleRepository.findByRoleCode(dto.getRoleCode())
                .orElseThrow(() -> new RuntimeException("Role not found with code: " + dto.getRoleCode()));

        // Validate officer exists
        Officer officer = officerRepository.findById(dto.getOfficerId())
                .orElseThrow(() -> new RuntimeException("Officer not found with ID: " + dto.getOfficerId()));

        // Validate unit level matches role level (except for DEALING_ASSISTANT)
        if (role.getUnitLevel() != null && !role.getUnitLevel().equals(unit.getUnitLevel())) {
            throw new IllegalArgumentException(
                    String.format("Role %s cannot be assigned to unit level %s. Expected level: %s",
                            role.getRoleCode(), unit.getUnitLevel(), role.getUnitLevel()));
        }

        // Step 1: Close existing active posting for same UNIT + ROLE
        closeExistingActivePosting(dto.getUnitId(), dto.getRoleCode());

        // Step 2: Generate UserID: ROLE_CODE@UNIT_LGD_CODE
        String userid = generateUserid(role.getRoleCode(), unit.getLgdCode());

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
        posting.setUnit(unit);
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
        log.info("Transferring person to new post - Unit: {}, Role: {}, Officer: {}", 
                dto.getUnitId(), dto.getRoleCode(), dto.getOfficerId());

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
    private void closeExistingActivePosting(Long unitId, String roleCode) {
        List<OfficerDaHistory> activePostings = postingRepository.findActivePostingsByUnitAndRole(unitId, roleCode);
        
        for (OfficerDaHistory posting : activePostings) {
            closePosting(posting.getId());
            log.info("Closed existing active posting - Unit: {}, Role: {}, Posting ID: {}", 
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
     * Generate UserID: ROLE_CODE@UNIT_LGD_CODE
     */
    private String generateUserid(String roleCode, Long lgdCode) {
        return roleCode + "@" + lgdCode;
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
     * Get all postings by unit
     */
    @Transactional(readOnly = true)
    public List<PostingDTO> getPostingsByUnit(Long unitId) {
        List<OfficerDaHistory> postings = postingRepository.findByUnitIdOrderByFromDateDesc(unitId);
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
        dto.setUnitId(posting.getUnitId());
        if (posting.getUnit() != null) {
            dto.setUnitName(posting.getUnit().getUnitName());
            dto.setUnitLgdCode(posting.getUnit().getLgdCode().toString());
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

