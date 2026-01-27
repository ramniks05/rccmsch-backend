package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.AvailableCourtsDTO;
import in.gov.manipur.rccms.dto.CourtDTO;
import in.gov.manipur.rccms.dto.CreateCourtDTO;
import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.entity.CaseType;
import in.gov.manipur.rccms.entity.Court;
import in.gov.manipur.rccms.entity.CourtLevel;
import in.gov.manipur.rccms.entity.CourtType;
import in.gov.manipur.rccms.exception.DuplicateUserException;
import in.gov.manipur.rccms.repository.AdminUnitRepository;
import in.gov.manipur.rccms.repository.CaseTypeRepository;
import in.gov.manipur.rccms.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Court Service
 * Handles CRUD operations for Courts
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourtService {

    private final CourtRepository courtRepository;
    private final AdminUnitRepository adminUnitRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create a new court
     */
    public CourtDTO createCourt(CreateCourtDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Court data cannot be null");
        }

        log.info("Creating court with code: {}", dto.getCourtCode());

        // Check if code already exists
        if (courtRepository.existsByCourtCode(dto.getCourtCode().toUpperCase().trim())) {
            log.warn("Court creation failed: Code {} already exists", dto.getCourtCode());
            throw new DuplicateUserException("Court code already exists");
        }

        // Validate unit exists
        AdminUnit unit = adminUnitRepository.findById(dto.getUnitId())
                .orElseThrow(() -> new RuntimeException("Admin unit not found with ID: " + dto.getUnitId()));

        // Validate court level matches unit level
        if (!unit.getUnitLevel().name().equals(dto.getCourtLevel().name())) {
            log.warn("Court level {} does not match unit level {}", dto.getCourtLevel(), unit.getUnitLevel());
            // Allow but log warning - some flexibility
        }

        // Create entity
        Court court = new Court();
        court.setCourtCode(dto.getCourtCode().toUpperCase().trim());
        court.setCourtName(dto.getCourtName().trim());
        court.setCourtLevel(dto.getCourtLevel());
        court.setCourtType(dto.getCourtType());
        court.setUnit(unit);
        court.setDesignation(dto.getDesignation() != null ? dto.getDesignation().trim() : null);
        court.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        court.setContactNumber(dto.getContactNumber());
        court.setEmail(dto.getEmail());
        court.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        Court saved = courtRepository.save(court);
        log.info("Court created successfully with ID: {}", saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Get court by ID
     */
    @Transactional(readOnly = true)
    public CourtDTO getCourtById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Court ID cannot be null");
        }

        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Court not found with ID: " + id));

        return convertToDTO(court);
    }

    /**
     * Get all active courts
     */
    @Transactional(readOnly = true)
    public List<CourtDTO> getActiveCourts() {
        List<Court> courts = courtRepository.findByIsActiveTrueOrderByCourtLevelAscCourtNameAsc();
        return courts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get courts by level
     */
    @Transactional(readOnly = true)
    public List<CourtDTO> getCourtsByLevel(CourtLevel level) {
        List<Court> courts = courtRepository.findByCourtLevelAndIsActiveTrueOrderByCourtNameAsc(level);
        return courts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get courts by unit ID
     */
    @Transactional(readOnly = true)
    public List<CourtDTO> getCourtsByUnit(Long unitId) {
        List<Court> courts = courtRepository.findByUnitIdAndIsActiveTrueOrderByCourtNameAsc(unitId);
        return courts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get available courts for a case type
     * This is the key method for frontend - returns courts based on case type selection
     */
    @Transactional(readOnly = true)
    public AvailableCourtsDTO getAvailableCourts(Long caseTypeId, Long unitId) {
        if (caseTypeId == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }
        if (unitId == null) {
            throw new IllegalArgumentException("Unit ID cannot be null");
        }

        // Get case type
        CaseType caseType = caseTypeRepository.findById(caseTypeId)
                .orElseThrow(() -> new RuntimeException("Case type not found with ID: " + caseTypeId));

        // Parse court types from JSON string
        List<String> courtTypeStrings;
        try {
            courtTypeStrings = objectMapper.readValue(caseType.getCourtTypes(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // If not JSON array, treat as single value
            courtTypeStrings = List.of(caseType.getCourtTypes());
        }

        // Convert to CourtType enum list
        List<CourtType> courtTypes = courtTypeStrings.stream()
                .map(CourtType::valueOf)
                .collect(Collectors.toList());

        // Get available courts
        List<Court> courts = courtRepository.findAvailableCourtsForCaseNature(
                caseType.getCourtLevel(),
                courtTypes,
                unitId
        );

        // If no courts found, try broader search (just by level and type, not unit hierarchy)
        if (courts.isEmpty()) {
            // Get courts by level, then filter by type
            List<Court> courtsByLevel = courtRepository.findByCourtLevelAndIsActiveTrueOrderByCourtNameAsc(caseType.getCourtLevel());
            courts = courtsByLevel.stream()
                    .filter(c -> courtTypes.contains(c.getCourtType()))
                    .collect(Collectors.toList());
        }

        log.info("Found {} available courts for case type {} and unit {}", courts.size(), caseTypeId, unitId);

        return AvailableCourtsDTO.builder()
                .caseType(convertCaseTypeToDTO(caseType))
                .courts(courts.stream().map(this::convertToDTO).collect(Collectors.toList()))
                .message("Available courts retrieved successfully")
                .build();
    }

    /**
     * Update court
     */
    public CourtDTO updateCourt(Long id, CreateCourtDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("Court ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Court data cannot be null");
        }

        log.info("Updating court with ID: {}", id);

        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Court not found with ID: " + id));

        // Check if code is being changed
        if (!court.getCourtCode().equalsIgnoreCase(dto.getCourtCode().trim())) {
            if (courtRepository.existsByCourtCode(dto.getCourtCode().toUpperCase().trim())) {
                log.warn("Court update failed: Code {} already exists", dto.getCourtCode());
                throw new DuplicateUserException("Court code already exists");
            }
            court.setCourtCode(dto.getCourtCode().toUpperCase().trim());
        }

        // Update unit if changed
        if (!court.getUnitId().equals(dto.getUnitId())) {
            AdminUnit unit = adminUnitRepository.findById(dto.getUnitId())
                    .orElseThrow(() -> new RuntimeException("Admin unit not found with ID: " + dto.getUnitId()));
            court.setUnit(unit);
        }

        // Update other fields
        court.setCourtName(dto.getCourtName().trim());
        court.setCourtLevel(dto.getCourtLevel());
        court.setCourtType(dto.getCourtType());
        court.setDesignation(dto.getDesignation() != null ? dto.getDesignation().trim() : null);
        court.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : null);
        court.setContactNumber(dto.getContactNumber());
        court.setEmail(dto.getEmail());
        if (dto.getIsActive() != null) {
            court.setIsActive(dto.getIsActive());
        }

        Court updated = courtRepository.save(court);
        log.info("Court updated successfully with ID: {}", updated.getId());

        return convertToDTO(updated);
    }

    /**
     * Delete court (soft delete)
     */
    public void deleteCourt(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Court ID cannot be null");
        }

        log.info("Deleting court with ID: {}", id);

        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Court not found with ID: " + id));

        // Soft delete
        court.setIsActive(false);
        courtRepository.save(court);

        log.info("Court deleted successfully (soft delete) with ID: {}", id);
    }

    /**
     * Convert Entity to DTO
     */
    private CourtDTO convertToDTO(Court court) {
        CourtDTO dto = new CourtDTO();
        dto.setId(court.getId());
        dto.setCourtCode(court.getCourtCode());
        dto.setCourtName(court.getCourtName());
        dto.setCourtLevel(court.getCourtLevel());
        dto.setCourtType(court.getCourtType());
        dto.setUnitId(court.getUnitId());
        if (court.getUnit() != null) {
            dto.setUnitName(court.getUnit().getUnitName());
            dto.setUnitCode(court.getUnit().getUnitCode());
        }
        dto.setDesignation(court.getDesignation());
        dto.setAddress(court.getAddress());
        dto.setContactNumber(court.getContactNumber());
        dto.setEmail(court.getEmail());
        dto.setIsActive(court.getIsActive());
        dto.setCreatedAt(court.getCreatedAt());
        dto.setUpdatedAt(court.getUpdatedAt());
        return dto;
    }

    /**
     * Convert CaseType to DTO (simplified)
     */
    private in.gov.manipur.rccms.dto.CaseTypeDTO convertCaseTypeToDTO(CaseType caseType) {
        in.gov.manipur.rccms.dto.CaseTypeDTO dto = new in.gov.manipur.rccms.dto.CaseTypeDTO();
        dto.setId(caseType.getId());
        dto.setCaseNatureId(caseType.getCaseNatureId());
        if (caseType.getCaseNature() != null) {
            dto.setCaseNatureName(caseType.getCaseNature().getName());
            dto.setCaseNatureCode(caseType.getCaseNature().getCode());
        }
        dto.setTypeCode(caseType.getTypeCode());
        dto.setTypeName(caseType.getTypeName());
        dto.setCourtLevel(caseType.getCourtLevel());
        // Parse court types
        try {
            List<String> courtTypes = objectMapper.readValue(caseType.getCourtTypes(), new TypeReference<List<String>>() {});
            dto.setCourtTypes(courtTypes);
        } catch (Exception e) {
            dto.setCourtTypes(List.of(caseType.getCourtTypes()));
        }
        dto.setFromLevel(caseType.getFromLevel());
        dto.setIsAppeal(caseType.getIsAppeal());
        dto.setAppealOrder(caseType.getAppealOrder());
        dto.setDescription(caseType.getDescription());
        dto.setIsActive(caseType.getIsActive());
        dto.setDisplayOrder(caseType.getDisplayOrder());
        return dto;
    }
}
