package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.ActDTO;
import in.gov.manipur.rccms.dto.CreateActDTO;
import in.gov.manipur.rccms.entity.Act;
import in.gov.manipur.rccms.exception.DuplicateUserException;
import in.gov.manipur.rccms.repository.ActRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Act Service
 * Handles CRUD operations for Acts
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ActService {

    private final ActRepository actRepository;

    /**
     * Create a new act
     */
    public ActDTO createAct(CreateActDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Act data cannot be null");
        }

        log.info("Creating act with code: {}", dto.getActCode());

        // Check if code already exists
        if (actRepository.existsByActCode(dto.getActCode().toUpperCase().trim())) {
            log.warn("Act creation failed: Code {} already exists", dto.getActCode());
            throw new DuplicateUserException("Act code already exists");
        }

        // Create entity
        Act act = new Act();
        act.setActCode(dto.getActCode().toUpperCase().trim());
        act.setActName(dto.getActName().trim());
        act.setActYear(dto.getActYear());
        act.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        act.setSections(dto.getSections());
        act.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        Act saved = actRepository.save(act);
        log.info("Act created successfully with ID: {}", saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Get act by ID
     */
    @Transactional(readOnly = true)
    public ActDTO getActById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Act ID cannot be null");
        }

        Act act = actRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Act not found with ID: " + id));

        return convertToDTO(act);
    }

    /**
     * Get act by code
     */
    @Transactional(readOnly = true)
    public ActDTO getActByCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Act code cannot be null or empty");
        }

        Act act = actRepository.findByActCode(code.toUpperCase().trim())
                .orElseThrow(() -> new RuntimeException("Act not found with code: " + code));

        return convertToDTO(act);
    }

    /**
     * Get all active acts
     */
    @Transactional(readOnly = true)
    public List<ActDTO> getActiveActs() {
        List<Act> acts = actRepository.findByIsActiveTrueOrderByActNameAsc();
        return acts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update act
     */
    public ActDTO updateAct(Long id, CreateActDTO dto) {
        if (id == null) {
            throw new IllegalArgumentException("Act ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Act data cannot be null");
        }

        log.info("Updating act with ID: {}", id);

        Act act = actRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Act not found with ID: " + id));

        // Check if code is being changed
        if (!act.getActCode().equalsIgnoreCase(dto.getActCode().trim())) {
            if (actRepository.existsByActCode(dto.getActCode().toUpperCase().trim())) {
                log.warn("Act update failed: Code {} already exists", dto.getActCode());
                throw new DuplicateUserException("Act code already exists");
            }
            act.setActCode(dto.getActCode().toUpperCase().trim());
        }

        // Update other fields
        act.setActName(dto.getActName().trim());
        act.setActYear(dto.getActYear());
        act.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        act.setSections(dto.getSections());
        if (dto.getIsActive() != null) {
            act.setIsActive(dto.getIsActive());
        }

        Act updated = actRepository.save(act);
        log.info("Act updated successfully with ID: {}", updated.getId());

        return convertToDTO(updated);
    }

    /**
     * Delete act (soft delete)
     */
    public void deleteAct(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Act ID cannot be null");
        }

        log.info("Deleting act with ID: {}", id);

        Act act = actRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Act not found with ID: " + id));

        // Soft delete
        act.setIsActive(false);
        actRepository.save(act);

        log.info("Act deleted successfully (soft delete) with ID: {}", id);
    }

    /**
     * Convert Entity to DTO
     */
    private ActDTO convertToDTO(Act act) {
        ActDTO dto = new ActDTO();
        dto.setId(act.getId());
        dto.setActCode(act.getActCode());
        dto.setActName(act.getActName());
        dto.setActYear(act.getActYear());
        dto.setDescription(act.getDescription());
        dto.setSections(act.getSections());
        dto.setIsActive(act.getIsActive());
        dto.setCreatedAt(act.getCreatedAt());
        dto.setUpdatedAt(act.getUpdatedAt());
        return dto;
    }
}
