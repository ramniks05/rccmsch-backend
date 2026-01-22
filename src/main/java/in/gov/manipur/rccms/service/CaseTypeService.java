package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.CaseTypeDTO;
import in.gov.manipur.rccms.entity.Act;
import in.gov.manipur.rccms.entity.CaseType;
import in.gov.manipur.rccms.exception.DuplicateUserException;
import in.gov.manipur.rccms.repository.ActRepository;
import in.gov.manipur.rccms.repository.CaseTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Case Type Service
 * Handles CRUD operations for case types
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CaseTypeService {

    private final CaseTypeRepository caseTypeRepository;
    private final ActRepository actRepository;

    /**
     * Create a new case type
     */
    public CaseTypeDTO createCaseType(CaseTypeDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Case type data cannot be null");
        }

        log.info("Creating case type with code: {}", dto.getCode());

        // Check if code already exists
        if (caseTypeRepository.existsByCode(dto.getCode().toUpperCase().trim())) {
            log.warn("Case type creation failed: Code {} already exists", dto.getCode());
            throw new DuplicateUserException("Case type code already exists");
        }

        // Check if name already exists
        if (caseTypeRepository.existsByName(dto.getName().trim())) {
            log.warn("Case type creation failed: Name {} already exists", dto.getName());
            throw new DuplicateUserException("Case type name already exists");
        }

        // Create entity
        CaseType caseType = new CaseType();
        caseType.setName(dto.getName().trim());
        caseType.setCode(dto.getCode().toUpperCase().trim());
        caseType.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        caseType.setWorkflowCode(dto.getWorkflowCode());
        caseType.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        
        // Set Act if provided
        if (dto.getActId() != null) {
            Act act = actRepository.findById(dto.getActId())
                    .orElseThrow(() -> new RuntimeException("Act not found with ID: " + dto.getActId()));
            caseType.setAct(act);
        }

        CaseType saved = caseTypeRepository.save(caseType);
        log.info("Case type created successfully with ID: {}", saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Get case type by ID
     */
    @Transactional(readOnly = true)
    public CaseTypeDTO getCaseTypeById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }

        CaseType caseType = caseTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Case type not found with ID: " + id));

        return convertToDTO(caseType);
    }

    /**
     * Get case type by code
     */
    @Transactional(readOnly = true)
    public CaseTypeDTO getCaseTypeByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Case type code cannot be null or empty");
        }

        CaseType caseType = caseTypeRepository.findByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new RuntimeException("Case type not found with code: " + code));

        return convertToDTO(caseType);
    }

    /**
     * Get all case types
     */
    @Transactional(readOnly = true)
    public List<CaseTypeDTO> getAllCaseTypes() {
        List<CaseType> caseTypes = caseTypeRepository.findAllByOrderByNameAsc();
        return caseTypes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all active case types
     */
    @Transactional(readOnly = true)
    public List<CaseTypeDTO> getActiveCaseTypes() {
        List<CaseType> caseTypes = caseTypeRepository.findByIsActiveTrueOrderByNameAsc();
        return caseTypes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update case type
     */
    public CaseTypeDTO updateCaseType(Long id, CaseTypeDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Case type data cannot be null");
        }

        log.info("Updating case type with ID: {}", id);

        CaseType caseType = caseTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Case type not found with ID: " + id));

        // Check if code is being changed and if new code already exists
        if (!caseType.getCode().equalsIgnoreCase(dto.getCode().trim())) {
            if (caseTypeRepository.existsByCode(dto.getCode().toUpperCase().trim())) {
                log.warn("Case type update failed: Code {} already exists", dto.getCode());
                throw new DuplicateUserException("Case type code already exists");
            }
            caseType.setCode(dto.getCode().toUpperCase().trim());
        }

        // Check if name is being changed and if new name already exists
        if (!caseType.getName().equals(dto.getName().trim())) {
            if (caseTypeRepository.existsByName(dto.getName().trim())) {
                log.warn("Case type update failed: Name {} already exists", dto.getName());
                throw new DuplicateUserException("Case type name already exists");
            }
            caseType.setName(dto.getName().trim());
        }

        // Update other fields
        caseType.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        caseType.setWorkflowCode(dto.getWorkflowCode());
        
        // Update Act if provided
        if (dto.getActId() != null) {
            Act act = actRepository.findById(dto.getActId())
                    .orElseThrow(() -> new RuntimeException("Act not found with ID: " + dto.getActId()));
            caseType.setAct(act);
        } else {
            caseType.setAct(null);
        }
        
        if (dto.getIsActive() != null) {
            caseType.setIsActive(dto.getIsActive());
        }

        CaseType updated = caseTypeRepository.save(caseType);
        log.info("Case type updated successfully with ID: {}", updated.getId());

        return convertToDTO(updated);
    }

    /**
     * Delete case type (soft delete by setting isActive to false)
     */
    public void deleteCaseType(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }

        log.info("Deleting case type with ID: {}", id);

        CaseType caseType = caseTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Case type not found with ID: " + id));

        // Soft delete
        caseType.setIsActive(false);
        caseTypeRepository.save(caseType);

        log.info("Case type deleted successfully (soft delete) with ID: {}", id);
    }

    /**
     * Hard delete case type (permanent deletion)
     */
    public void hardDeleteCaseType(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }

        log.info("Hard deleting case type with ID: {}", id);

        if (!caseTypeRepository.existsById(id)) {
            throw new RuntimeException("Case type not found with ID: " + id);
        }

        caseTypeRepository.deleteById(id);
        log.info("Case type hard deleted successfully with ID: {}", id);
    }

    /**
     * Convert Entity to DTO
     */
    private CaseTypeDTO convertToDTO(CaseType caseType) {
        CaseTypeDTO dto = new CaseTypeDTO();
        dto.setId(caseType.getId());
        dto.setName(caseType.getName());
        dto.setCode(caseType.getCode());
        dto.setDescription(caseType.getDescription());
        dto.setWorkflowCode(caseType.getWorkflowCode());
        dto.setIsActive(caseType.getIsActive());
        dto.setCreatedAt(caseType.getCreatedAt());
        dto.setUpdatedAt(caseType.getUpdatedAt());
        
        // Include Act information
        if (caseType.getAct() != null) {
            dto.setActId(caseType.getAct().getId());
            dto.setActName(caseType.getAct().getActName());
        dto.setActCode(caseType.getAct().getActCode());
        dto.setActYear(caseType.getAct().getActYear());
        }
        dto.setWorkflowCode(caseType.getWorkflowCode());
        
        return dto;
    }
}

