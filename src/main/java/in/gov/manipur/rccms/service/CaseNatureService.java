package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.CaseNatureDTO;
import in.gov.manipur.rccms.entity.Act;
import in.gov.manipur.rccms.entity.CaseNature;
import in.gov.manipur.rccms.exception.DuplicateUserException;
import in.gov.manipur.rccms.repository.ActRepository;
import in.gov.manipur.rccms.repository.CaseNatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Case Nature Service (Previously CaseTypeService)
 * Handles CRUD operations for case natures (MUTATION_GIFT_SALE, PARTITION, etc.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CaseNatureService {

    private final CaseNatureRepository caseNatureRepository;
    private final ActRepository actRepository;

    /**
     * Create a new case nature
     */
    public CaseNatureDTO createCaseNature(CaseNatureDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Case nature data cannot be null");
        }

        log.info("Creating case nature with code: {}", dto.getCode());

        // Check if code already exists
        if (caseNatureRepository.existsByCode(dto.getCode().toUpperCase().trim())) {
            log.warn("Case nature creation failed: Code {} already exists", dto.getCode());
            throw new DuplicateUserException("Case nature code already exists");
        }

        // Check if name already exists
        if (caseNatureRepository.existsByName(dto.getName().trim())) {
            log.warn("Case nature creation failed: Name {} already exists", dto.getName());
            throw new DuplicateUserException("Case nature name already exists");
        }

        // Create entity
        CaseNature caseNature = new CaseNature();
        caseNature.setName(dto.getName().trim());
        caseNature.setCode(dto.getCode().toUpperCase().trim());
        caseNature.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        caseNature.setWorkflowCode(dto.getWorkflowCode());
        caseNature.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        // Set Act if provided
        if (dto.getActId() != null) {
            Act act = actRepository.findById(dto.getActId())
                    .orElseThrow(() -> new RuntimeException("Act not found with ID: " + dto.getActId()));
            caseNature.setAct(act);
        }

        CaseNature saved = caseNatureRepository.save(caseNature);
        log.info("Case nature created successfully with ID: {}", saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Get case nature by ID
     */
    @Transactional(readOnly = true)
    public CaseNatureDTO getCaseNatureById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }

        CaseNature caseNature = caseNatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Case nature not found with ID: " + id));

        return convertToDTO(caseNature);
    }

    /**
     * Get case nature by code
     */
    @Transactional(readOnly = true)
    public CaseNatureDTO getCaseNatureByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Case nature code cannot be null or empty");
        }

        CaseNature caseNature = caseNatureRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new RuntimeException("Case nature not found with code: " + code));

        return convertToDTO(caseNature);
    }

    /**
     * Get all case natures
     */
    @Transactional(readOnly = true)
    public List<CaseNatureDTO> getAllCaseNatures() {
        List<CaseNature> caseNatures = caseNatureRepository.findAllByOrderByNameAsc();
        return caseNatures.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all active case natures
     */
    @Transactional(readOnly = true)
    public List<CaseNatureDTO> getActiveCaseNatures() {
        List<CaseNature> caseNatures = caseNatureRepository.findByIsActiveTrueOrderByNameAsc();
        return caseNatures.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update case nature
     */
    public CaseNatureDTO updateCaseNature(Long id, CaseNatureDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Case nature data cannot be null");
        }

        log.info("Updating case nature with ID: {}", id);

        CaseNature caseNature = caseNatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Case nature not found with ID: " + id));

        // Check if code is being changed and if new code already exists
        if (!caseNature.getCode().equalsIgnoreCase(dto.getCode().trim())) {
            if (caseNatureRepository.existsByCode(dto.getCode().toUpperCase().trim())) {
                log.warn("Case nature update failed: Code {} already exists", dto.getCode());
                throw new DuplicateUserException("Case nature code already exists");
            }
            caseNature.setCode(dto.getCode().toUpperCase().trim());
        }

        // Check if name is being changed and if new name already exists
        if (!caseNature.getName().equals(dto.getName().trim())) {
            if (caseNatureRepository.existsByName(dto.getName().trim())) {
                log.warn("Case nature update failed: Name {} already exists", dto.getName());
                throw new DuplicateUserException("Case nature name already exists");
            }
            caseNature.setName(dto.getName().trim());
        }

        // Update other fields
        caseNature.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        caseNature.setWorkflowCode(dto.getWorkflowCode());
        
        // Update Act if provided
        if (dto.getActId() != null) {
            Act act = actRepository.findById(dto.getActId())
                    .orElseThrow(() -> new RuntimeException("Act not found with ID: " + dto.getActId()));
            caseNature.setAct(act);
        } else {
            caseNature.setAct(null);
        }
        
        if (dto.getIsActive() != null) {
            caseNature.setIsActive(dto.getIsActive());
        }

        CaseNature updated = caseNatureRepository.save(caseNature);
        log.info("Case nature updated successfully with ID: {}", updated.getId());

        return convertToDTO(updated);
    }

    /**
     * Delete case nature (soft delete by setting isActive to false)
     */
    public void deleteCaseNature(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }

        log.info("Deleting case nature with ID: {}", id);

        CaseNature caseNature = caseNatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Case nature not found with ID: " + id));

        // Soft delete
        caseNature.setIsActive(false);
        caseNatureRepository.save(caseNature);

        log.info("Case nature deleted successfully (soft delete) with ID: {}", id);
    }

    /**
     * Convert Entity to DTO
     */
    private CaseNatureDTO convertToDTO(CaseNature caseNature) {
        CaseNatureDTO dto = new CaseNatureDTO();
        dto.setId(caseNature.getId());
        dto.setName(caseNature.getName());
        dto.setCode(caseNature.getCode());
        dto.setDescription(caseNature.getDescription());
        dto.setWorkflowCode(caseNature.getWorkflowCode());
        dto.setIsActive(caseNature.getIsActive());
        dto.setCreatedAt(caseNature.getCreatedAt());
        dto.setUpdatedAt(caseNature.getUpdatedAt());
        
        // Include Act information
        if (caseNature.getAct() != null) {
            dto.setActId(caseNature.getAct().getId());
            dto.setActName(caseNature.getAct().getActName());
            dto.setActCode(caseNature.getAct().getActCode());
            dto.setActYear(caseNature.getAct().getActYear());
        }
        
        return dto;
    }
}
