package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.CaseTypeDTO;
import in.gov.manipur.rccms.dto.CreateCaseTypeDTO;
import in.gov.manipur.rccms.entity.CaseNature;
import in.gov.manipur.rccms.entity.CaseType;
import in.gov.manipur.rccms.exception.DuplicateUserException;
import in.gov.manipur.rccms.repository.CaseNatureRepository;
import in.gov.manipur.rccms.repository.CaseTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Case Type Service (Previously CaseNatureService)
 * Handles CRUD operations for Case Types (NEW_FILE, APPEAL, REVISION, etc.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CaseTypeService {

    private final CaseTypeRepository caseTypeRepository;
    private final CaseNatureRepository caseNatureRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create a new case type
     */
    public CaseTypeDTO createCaseType(CreateCaseTypeDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Case type data cannot be null");
        }

        log.info("Creating case type with code: {} for case nature: {}", dto.getTypeCode(), dto.getCaseNatureId());

        // Validate case nature exists
        CaseNature caseNature = caseNatureRepository.findById(dto.getCaseNatureId())
                .orElseThrow(() -> new RuntimeException("Case nature not found with ID: " + dto.getCaseNatureId()));

        // Check if type code already exists for this case nature
        if (caseTypeRepository.existsByTypeCodeAndCaseNatureId(
                dto.getTypeCode().toUpperCase().trim(), dto.getCaseNatureId())) {
            log.warn("Case type creation failed: Code {} already exists for case nature {}", 
                    dto.getTypeCode(), dto.getCaseNatureId());
            throw new DuplicateUserException("Case type code already exists for this case nature");
        }

        // Convert court types list to JSON string
        String courtTypesJson;
        try {
            courtTypesJson = objectMapper.writeValueAsString(dto.getCourtTypes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert court types to JSON", e);
        }

        // Create entity
        CaseType caseType = new CaseType();
        caseType.setCaseNature(caseNature);
        caseType.setTypeCode(dto.getTypeCode().toUpperCase().trim());
        caseType.setTypeName(dto.getTypeName().trim());
        caseType.setCourtLevel(dto.getCourtLevel());
        caseType.setCourtTypes(courtTypesJson);
        caseType.setFromLevel(dto.getFromLevel());
        caseType.setIsAppeal(dto.getIsAppeal() != null ? dto.getIsAppeal() : false);
        caseType.setAppealOrder(dto.getAppealOrder() != null ? dto.getAppealOrder() : 0);
        caseType.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        caseType.setWorkflowCode(dto.getWorkflowCode() != null ? dto.getWorkflowCode().trim() : null);
        caseType.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        caseType.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);

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
     * Get all active case types by case nature ID
     */
    @Transactional(readOnly = true)
    public List<CaseTypeDTO> getCaseTypesByCaseNature(Long caseNatureId) {
        // Use eager fetch to avoid lazy loading issues
        List<CaseType> caseTypes = caseTypeRepository.findByCaseNatureIdAndIsActiveTrueWithCaseNature(caseNatureId);
        return caseTypes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all active case types
     */
    @Transactional(readOnly = true)
    public List<CaseTypeDTO> getAllCaseTypes() {
        // Use eager fetch to avoid lazy loading issues
        List<CaseType> allCaseTypes = caseTypeRepository.findAllActiveWithCaseNature();
        return allCaseTypes.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Update case type
     */
    public CaseTypeDTO updateCaseType(Long id, CreateCaseTypeDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Case type data cannot be null");
        }

        log.info("Updating case type with ID: {}", id);

        CaseType caseType = caseTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Case type not found with ID: " + id));

        // Check if code is being changed
        if (!caseType.getTypeCode().equalsIgnoreCase(dto.getTypeCode().trim())) {
            if (caseTypeRepository.existsByTypeCodeAndCaseNatureId(
                    dto.getTypeCode().toUpperCase().trim(), dto.getCaseNatureId())) {
                log.warn("Case type update failed: Code {} already exists", dto.getTypeCode());
                throw new DuplicateUserException("Case type code already exists");
            }
            caseType.setTypeCode(dto.getTypeCode().toUpperCase().trim());
        }

        // Update case nature if changed
        if (!caseType.getCaseNatureId().equals(dto.getCaseNatureId())) {
            CaseNature caseNature = caseNatureRepository.findById(dto.getCaseNatureId())
                    .orElseThrow(() -> new RuntimeException("Case nature not found with ID: " + dto.getCaseNatureId()));
            caseType.setCaseNature(caseNature);
        }

        // Convert court types list to JSON string
        String courtTypesJson;
        try {
            courtTypesJson = objectMapper.writeValueAsString(dto.getCourtTypes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert court types to JSON", e);
        }

        // Update other fields
        caseType.setTypeName(dto.getTypeName().trim());
        caseType.setCourtLevel(dto.getCourtLevel());
        caseType.setCourtTypes(courtTypesJson);
        caseType.setFromLevel(dto.getFromLevel());
        caseType.setIsAppeal(dto.getIsAppeal() != null ? dto.getIsAppeal() : false);
        caseType.setAppealOrder(dto.getAppealOrder() != null ? dto.getAppealOrder() : 0);
        caseType.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        caseType.setWorkflowCode(dto.getWorkflowCode() != null ? dto.getWorkflowCode().trim() : null);
        caseType.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        if (dto.getIsActive() != null) {
            caseType.setIsActive(dto.getIsActive());
        }

        CaseType updated = caseTypeRepository.save(caseType);
        log.info("Case type updated successfully with ID: {}", updated.getId());

        return convertToDTO(updated);
    }

    /**
     * Delete case type (soft delete)
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
     * Convert Entity to DTO
     */
    private CaseTypeDTO convertToDTO(CaseType caseType) {
        CaseTypeDTO dto = new CaseTypeDTO();
        dto.setId(caseType.getId());
        dto.setCaseNatureId(caseType.getCaseNatureId());
        
        // Handle lazy-loaded CaseNature relationship
        try {
            if (caseType.getCaseNature() != null) {
                dto.setCaseNatureName(caseType.getCaseNature().getName());
                dto.setCaseNatureCode(caseType.getCaseNature().getCode());
            }
        } catch (Exception e) {
            log.warn("Could not load CaseNature for CaseType {}: {}", caseType.getId(), e.getMessage());
            // CaseNature might be lazy-loaded and not initialized, use caseNatureId only
        }
        
        dto.setTypeCode(caseType.getTypeCode());
        dto.setTypeName(caseType.getTypeName());
        dto.setCourtLevel(caseType.getCourtLevel());
        
        // Parse court types from JSON
        String courtTypesJson = caseType.getCourtTypes();
        if (courtTypesJson != null && !courtTypesJson.trim().isEmpty()) {
            try {
                List<String> courtTypes = objectMapper.readValue(
                        courtTypesJson, 
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}
                );
                dto.setCourtTypes(courtTypes);
            } catch (Exception e) {
                // If not JSON array, treat as single value
                dto.setCourtTypes(List.of(courtTypesJson));
            }
        } else {
            dto.setCourtTypes(List.of());
        }
        
        dto.setFromLevel(caseType.getFromLevel());
        dto.setIsAppeal(caseType.getIsAppeal() != null ? caseType.getIsAppeal() : false);
        dto.setAppealOrder(caseType.getAppealOrder() != null ? caseType.getAppealOrder() : 0);
        dto.setDescription(caseType.getDescription());
        dto.setWorkflowCode(caseType.getWorkflowCode());
        dto.setIsActive(caseType.getIsActive() != null ? caseType.getIsActive() : true);
        dto.setDisplayOrder(caseType.getDisplayOrder() != null ? caseType.getDisplayOrder() : 0);
        dto.setCreatedAt(caseType.getCreatedAt());
        dto.setUpdatedAt(caseType.getUpdatedAt());
        return dto;
    }
}
