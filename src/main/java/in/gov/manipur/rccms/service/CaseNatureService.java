package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.CaseNatureDTO;
import in.gov.manipur.rccms.dto.CreateCaseNatureDTO;
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
 * Case Nature Service
 * Handles CRUD operations for Case Natures
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CaseNatureService {

    private final CaseNatureRepository caseNatureRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create a new case nature
     */
    public CaseNatureDTO createCaseNature(CreateCaseNatureDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Case nature data cannot be null");
        }

        log.info("Creating case nature with code: {} for case type: {}", dto.getNatureCode(), dto.getCaseTypeId());

        // Validate case type exists
        CaseType caseType = caseTypeRepository.findById(dto.getCaseTypeId())
                .orElseThrow(() -> new RuntimeException("Case type not found with ID: " + dto.getCaseTypeId()));

        // Check if nature code already exists for this case type
        if (caseNatureRepository.existsByNatureCodeAndCaseTypeId(
                dto.getNatureCode().toUpperCase().trim(), dto.getCaseTypeId())) {
            log.warn("Case nature creation failed: Code {} already exists for case type {}", 
                    dto.getNatureCode(), dto.getCaseTypeId());
            throw new DuplicateUserException("Case nature code already exists for this case type");
        }

        // Convert court types list to JSON string
        String courtTypesJson;
        try {
            courtTypesJson = objectMapper.writeValueAsString(dto.getCourtTypes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert court types to JSON", e);
        }

        // Create entity
        CaseNature caseNature = new CaseNature();
        caseNature.setCaseType(caseType);
        caseNature.setNatureCode(dto.getNatureCode().toUpperCase().trim());
        caseNature.setNatureName(dto.getNatureName().trim());
        caseNature.setCourtLevel(dto.getCourtLevel());
        caseNature.setCourtTypes(courtTypesJson);
        caseNature.setFromLevel(dto.getFromLevel());
        caseNature.setIsAppeal(dto.getIsAppeal() != null ? dto.getIsAppeal() : false);
        caseNature.setAppealOrder(dto.getAppealOrder() != null ? dto.getAppealOrder() : 0);
        caseNature.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        caseNature.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        caseNature.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);

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
     * Get all active case natures by case type ID
     */
    @Transactional(readOnly = true)
    public List<CaseNatureDTO> getCaseNaturesByCaseType(Long caseTypeId) {
        List<CaseNature> caseNatures = caseNatureRepository.findByCaseTypeIdAndIsActiveTrueOrderByDisplayOrderAscNatureNameAsc(caseTypeId);
        return caseNatures.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all active case natures
     */
    @Transactional(readOnly = true)
    public List<CaseNatureDTO> getAllCaseNatures() {
        List<CaseNature> caseNatures = caseNatureRepository.findByIsActiveTrueOrderByDisplayOrderAscNatureNameAsc();
        return caseNatures.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update case nature
     */
    public CaseNatureDTO updateCaseNature(Long id, CreateCaseNatureDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Case nature data cannot be null");
        }

        log.info("Updating case nature with ID: {}", id);

        CaseNature caseNature = caseNatureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Case nature not found with ID: " + id));

        // Check if code is being changed
        if (!caseNature.getNatureCode().equalsIgnoreCase(dto.getNatureCode().trim())) {
            if (caseNatureRepository.existsByNatureCodeAndCaseTypeId(
                    dto.getNatureCode().toUpperCase().trim(), dto.getCaseTypeId())) {
                log.warn("Case nature update failed: Code {} already exists", dto.getNatureCode());
                throw new DuplicateUserException("Case nature code already exists");
            }
            caseNature.setNatureCode(dto.getNatureCode().toUpperCase().trim());
        }

        // Update case type if changed
        if (!caseNature.getCaseTypeId().equals(dto.getCaseTypeId())) {
            CaseType caseType = caseTypeRepository.findById(dto.getCaseTypeId())
                    .orElseThrow(() -> new RuntimeException("Case type not found with ID: " + dto.getCaseTypeId()));
            caseNature.setCaseType(caseType);
        }

        // Convert court types list to JSON string
        String courtTypesJson;
        try {
            courtTypesJson = objectMapper.writeValueAsString(dto.getCourtTypes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert court types to JSON", e);
        }

        // Update other fields
        caseNature.setNatureName(dto.getNatureName().trim());
        caseNature.setCourtLevel(dto.getCourtLevel());
        caseNature.setCourtTypes(courtTypesJson);
        caseNature.setFromLevel(dto.getFromLevel());
        caseNature.setIsAppeal(dto.getIsAppeal() != null ? dto.getIsAppeal() : false);
        caseNature.setAppealOrder(dto.getAppealOrder() != null ? dto.getAppealOrder() : 0);
        caseNature.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        caseNature.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        if (dto.getIsActive() != null) {
            caseNature.setIsActive(dto.getIsActive());
        }

        CaseNature updated = caseNatureRepository.save(caseNature);
        log.info("Case nature updated successfully with ID: {}", updated.getId());

        return convertToDTO(updated);
    }

    /**
     * Delete case nature (soft delete)
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
        dto.setCaseTypeId(caseNature.getCaseTypeId());
        if (caseNature.getCaseType() != null) {
            dto.setCaseTypeName(caseNature.getCaseType().getName());
            dto.setCaseTypeCode(caseNature.getCaseType().getCode());
        }
        dto.setNatureCode(caseNature.getNatureCode());
        dto.setNatureName(caseNature.getNatureName());
        dto.setCourtLevel(caseNature.getCourtLevel());
        
        // Parse court types from JSON
        try {
            List<String> courtTypes = objectMapper.readValue(
                    caseNature.getCourtTypes(), 
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}
            );
            dto.setCourtTypes(courtTypes);
        } catch (Exception e) {
            // If not JSON array, treat as single value
            dto.setCourtTypes(List.of(caseNature.getCourtTypes()));
        }
        
        dto.setFromLevel(caseNature.getFromLevel());
        dto.setIsAppeal(caseNature.getIsAppeal());
        dto.setAppealOrder(caseNature.getAppealOrder());
        dto.setDescription(caseNature.getDescription());
        dto.setIsActive(caseNature.getIsActive());
        dto.setDisplayOrder(caseNature.getDisplayOrder());
        dto.setCreatedAt(caseNature.getCreatedAt());
        dto.setUpdatedAt(caseNature.getUpdatedAt());
        return dto;
    }
}
