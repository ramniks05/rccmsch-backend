package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.AdminUnitDTO;
import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.exception.DuplicateUserException;
import in.gov.manipur.rccms.repository.AdminUnitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Administrative Unit Service
 * Handles CRUD operations for administrative units
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminUnitService {

    private final AdminUnitRepository adminUnitRepository;

    /**
     * Create a new administrative unit
     */
    public AdminUnitDTO createAdminUnit(AdminUnitDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Admin unit data cannot be null");
        }

        log.info("Creating admin unit with code: {}", dto.getUnitCode());

        // Check if code already exists
        if (adminUnitRepository.existsByUnitCode(dto.getUnitCode().toUpperCase().trim())) {
            log.warn("Admin unit creation failed: Code {} already exists", dto.getUnitCode());
            throw new DuplicateUserException("Admin unit code already exists");
        }

        // Check if LGD code already exists
        if (dto.getLgdCode() != null && adminUnitRepository.existsByLgdCode(dto.getLgdCode())) {
            log.warn("Admin unit creation failed: LGD code {} already exists", dto.getLgdCode());
            throw new DuplicateUserException("LGD code already exists");
        }

        // Create entity
        AdminUnit adminUnit = new AdminUnit();
        adminUnit.setUnitCode(dto.getUnitCode().toUpperCase().trim());
        adminUnit.setUnitName(dto.getUnitName().trim());
        adminUnit.setUnitLevel(dto.getUnitLevel());
        adminUnit.setLgdCode(dto.getLgdCode());
        adminUnit.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        // Set parent unit if provided
        if (dto.getParentUnitId() != null) {
            AdminUnit parent = adminUnitRepository.findById(dto.getParentUnitId())
                    .orElseThrow(() -> new RuntimeException("Parent unit not found with ID: " + dto.getParentUnitId()));
            adminUnit.setParentUnit(parent);
        }

        AdminUnit saved = adminUnitRepository.save(adminUnit);
        log.info("Admin unit created successfully with ID: {}", saved.getUnitId());

        return convertToDTO(saved);
    }

    /**
     * Get admin unit by ID
     */
    @Transactional(readOnly = true)
    public AdminUnitDTO getAdminUnitById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Admin unit ID cannot be null");
        }

        AdminUnit adminUnit = adminUnitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin unit not found with ID: " + id));

        return convertToDTO(adminUnit);
    }

    /**
     * Get admin unit by code
     */
    @Transactional(readOnly = true)
    public AdminUnitDTO getAdminUnitByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Admin unit code cannot be null or empty");
        }

        AdminUnit adminUnit = adminUnitRepository.findByUnitCode(code.toUpperCase().trim())
                .orElseThrow(() -> new RuntimeException("Admin unit not found with code: " + code));

        return convertToDTO(adminUnit);
    }

    /**
     * Get all admin units
     */
    @Transactional(readOnly = true)
    public List<AdminUnitDTO> getAllAdminUnits() {
        List<AdminUnit> adminUnits = adminUnitRepository.findAllByOrderByUnitLevelAscUnitNameAsc();
        return adminUnits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all active admin units
     */
    @Transactional(readOnly = true)
    public List<AdminUnitDTO> getActiveAdminUnits() {
        List<AdminUnit> adminUnits = adminUnitRepository.findByIsActiveTrueOrderByUnitLevelAscUnitNameAsc();
        return adminUnits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get admin units by level
     */
    @Transactional(readOnly = true)
    public List<AdminUnitDTO> getAdminUnitsByLevel(AdminUnit.UnitLevel level) {
        List<AdminUnit> adminUnits = adminUnitRepository.findByUnitLevelAndIsActiveTrue(level);
        return adminUnits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get admin units by parent unit ID
     */
    @Transactional(readOnly = true)
    public List<AdminUnitDTO> getAdminUnitsByParent(Long parentUnitId) {
        List<AdminUnit> adminUnits = adminUnitRepository.findByParentUnitIdAndIsActiveTrue(parentUnitId);
        return adminUnits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get root units (State level)
     */
    @Transactional(readOnly = true)
    public List<AdminUnitDTO> getRootUnits() {
        List<AdminUnit> adminUnits = adminUnitRepository.findRootUnits();
        return adminUnits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update admin unit
     */
    public AdminUnitDTO updateAdminUnit(Long id, AdminUnitDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("Admin unit ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Admin unit data cannot be null");
        }

        log.info("Updating admin unit with ID: {}", id);

        AdminUnit adminUnit = adminUnitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin unit not found with ID: " + id));

        // Check if code is being changed and if new code already exists
        if (!adminUnit.getUnitCode().equalsIgnoreCase(dto.getUnitCode().trim())) {
            if (adminUnitRepository.existsByUnitCode(dto.getUnitCode().toUpperCase().trim())) {
                log.warn("Admin unit update failed: Code {} already exists", dto.getUnitCode());
                throw new DuplicateUserException("Admin unit code already exists");
            }
            adminUnit.setUnitCode(dto.getUnitCode().toUpperCase().trim());
        }

        // Check if LGD code is being changed and if new LGD code already exists
        if (dto.getLgdCode() != null && !adminUnit.getLgdCode().equals(dto.getLgdCode())) {
            if (adminUnitRepository.existsByLgdCode(dto.getLgdCode())) {
                log.warn("Admin unit update failed: LGD code {} already exists", dto.getLgdCode());
                throw new DuplicateUserException("LGD code already exists");
            }
            adminUnit.setLgdCode(dto.getLgdCode());
        }

        // Update other fields
        adminUnit.setUnitName(dto.getUnitName().trim());
        adminUnit.setUnitLevel(dto.getUnitLevel());
        if (dto.getIsActive() != null) {
            adminUnit.setIsActive(dto.getIsActive());
        }

        // Update parent unit if provided
        if (dto.getParentUnitId() != null) {
            AdminUnit parent = adminUnitRepository.findById(dto.getParentUnitId())
                    .orElseThrow(() -> new RuntimeException("Parent unit not found with ID: " + dto.getParentUnitId()));
            adminUnit.setParentUnit(parent);
        } else {
            adminUnit.setParentUnit(null);
        }

        AdminUnit updated = adminUnitRepository.save(adminUnit);
        log.info("Admin unit updated successfully with ID: {}", updated.getUnitId());

        return convertToDTO(updated);
    }

    /**
     * Delete admin unit (soft delete)
     */
    public void deleteAdminUnit(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Admin unit ID cannot be null");
        }

        log.info("Deleting admin unit with ID: {}", id);

        AdminUnit adminUnit = adminUnitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin unit not found with ID: " + id));

        // Soft delete
        adminUnit.setIsActive(false);
        adminUnitRepository.save(adminUnit);

        log.info("Admin unit deleted successfully (soft delete) with ID: {}", id);
    }

    /**
     * Convert Entity to DTO
     */
    private AdminUnitDTO convertToDTO(AdminUnit adminUnit) {
        AdminUnitDTO dto = new AdminUnitDTO();
        dto.setUnitId(adminUnit.getUnitId());
        dto.setUnitCode(adminUnit.getUnitCode());
        dto.setUnitName(adminUnit.getUnitName());
        dto.setUnitLevel(adminUnit.getUnitLevel());
        dto.setLgdCode(adminUnit.getLgdCode());
        dto.setParentUnitId(adminUnit.getParentUnitId());
        if (adminUnit.getParentUnit() != null) {
            dto.setParentUnitName(adminUnit.getParentUnit().getUnitName());
        }
        dto.setIsActive(adminUnit.getIsActive());
        return dto;
    }
}

