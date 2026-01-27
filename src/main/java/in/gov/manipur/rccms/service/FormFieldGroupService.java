package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.CreateFormFieldGroupDTO;
import in.gov.manipur.rccms.dto.FormFieldGroupDTO;
import in.gov.manipur.rccms.entity.CaseType;
import in.gov.manipur.rccms.entity.FormFieldGroup;
import in.gov.manipur.rccms.repository.CaseTypeRepository;
import in.gov.manipur.rccms.repository.FormFieldGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Form Field Group Service
 * Manages master field groups for case type form schemas
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FormFieldGroupService {

    private final FormFieldGroupRepository groupRepository;
    private final CaseTypeRepository caseTypeRepository;

    /**
     * Get all active groups for a case type
     */
    @Transactional(readOnly = true)
    public List<FormFieldGroupDTO> getActiveGroups(Long caseTypeId) {
        if (caseTypeId == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }
        List<FormFieldGroup> groups = groupRepository.findActiveGroupsByCaseTypeId(caseTypeId);
        return toDtos(groups);
    }

    /**
     * Get all groups (active and inactive) for a case type
     */
    @Transactional(readOnly = true)
    public List<FormFieldGroupDTO> getAllGroups(Long caseTypeId) {
        if (caseTypeId == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }
        List<FormFieldGroup> groups = groupRepository.findAllGroupsByCaseTypeId(caseTypeId);
        return toDtos(groups);
    }

    /**
     * Get group by ID
     */
    @Transactional(readOnly = true)
    public FormFieldGroupDTO getGroupById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }
        FormFieldGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Form field group not found: " + id));
        return toDto(group);
    }

    /**
     * Create a new group
     */
    public FormFieldGroupDTO createGroup(CreateFormFieldGroupDTO dto) {
        log.info("Creating form field group: caseTypeId={}, groupCode={}", dto.getCaseTypeId(), dto.getGroupCode());

        if (dto.getCaseTypeId() == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }

        // Validate case type exists
        CaseType caseType = caseTypeRepository.findById(dto.getCaseTypeId())
                .orElseThrow(() -> new RuntimeException("Case type not found: " + dto.getCaseTypeId()));

        // Check if group code already exists for this case type
        if (groupRepository.existsByCaseTypeIdAndGroupCode(dto.getCaseTypeId(), dto.getGroupCode())) {
            throw new RuntimeException("Group code already exists for this case type: " + dto.getGroupCode());
        }

        FormFieldGroup group = new FormFieldGroup();
        group.setCaseType(caseType);
        group.setCaseTypeId(caseType.getId());
        group.setGroupCode(dto.getGroupCode());
        group.setGroupLabel(dto.getGroupLabel());
        group.setDescription(dto.getDescription());
        group.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        group.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        FormFieldGroup saved = groupRepository.save(group);
        log.info("Form field group created successfully: id={}", saved.getId());

        return toDto(saved);
    }

    /**
     * Update a group
     */
    public FormFieldGroupDTO updateGroup(Long groupId, CreateFormFieldGroupDTO dto) {
        log.info("Updating form field group: id={}", groupId);

        if (groupId == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }

        FormFieldGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Form field group not found: " + groupId));

        // If group code is being changed, check if new code already exists
        if (!group.getGroupCode().equals(dto.getGroupCode())) {
            if (groupRepository.existsByCaseTypeIdAndGroupCode(dto.getCaseTypeId(), dto.getGroupCode())) {
                throw new RuntimeException("Group code already exists for this case type: " + dto.getGroupCode());
            }
        }

        // Update fields
        group.setGroupCode(dto.getGroupCode());
        group.setGroupLabel(dto.getGroupLabel());
        group.setDescription(dto.getDescription());
        if (dto.getDisplayOrder() != null) {
            group.setDisplayOrder(dto.getDisplayOrder());
        }
        if (dto.getIsActive() != null) {
            group.setIsActive(dto.getIsActive());
        }

        FormFieldGroup saved = groupRepository.save(group);
        log.info("Form field group updated successfully: id={}", saved.getId());

        return toDto(saved);
    }

    /**
     * Delete a group
     */
    public void deleteGroup(Long groupId) {
        log.info("Deleting form field group: id={}", groupId);

        if (groupId == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }

        FormFieldGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Form field group not found: " + groupId));

        // TODO: Check if any fields are using this group before deleting
        // For now, allow deletion - fields will have orphaned group codes

        groupRepository.delete(group);
        log.info("Form field group deleted successfully: id={}", groupId);
    }

    /**
     * Check if a group exists and is active for a case type
     */
    @Transactional(readOnly = true)
    public boolean existsActiveGroup(Long caseTypeId, String groupCode) {
        if (groupCode == null || groupCode.trim().isEmpty()) {
            return true; // Empty group code is allowed (no group)
        }
        if (caseTypeId == null) {
            return false;
        }
        return groupRepository.findByCaseTypeIdAndGroupCode(caseTypeId, groupCode)
                .map(FormFieldGroup::getIsActive)
                .orElse(false);
    }

    /**
     * Convert entities to DTOs
     */
    private List<FormFieldGroupDTO> toDtos(List<FormFieldGroup> groups) {
        List<FormFieldGroupDTO> dtos = new ArrayList<>();
        for (FormFieldGroup group : groups) {
            dtos.add(toDto(group));
        }
        return dtos;
    }

    /**
     * Convert entity to DTO
     */
    private FormFieldGroupDTO toDto(FormFieldGroup group) {
        FormFieldGroupDTO dto = new FormFieldGroupDTO();
        dto.setId(group.getId());
        dto.setCaseTypeId(group.getCaseTypeId());
        dto.setGroupCode(group.getGroupCode());
        dto.setGroupLabel(group.getGroupLabel());
        dto.setDescription(group.getDescription());
        dto.setDisplayOrder(group.getDisplayOrder());
        dto.setIsActive(group.getIsActive());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());

        if (group.getCaseType() != null) {
            dto.setCaseTypeName(group.getCaseType().getTypeName());
            dto.setCaseTypeCode(group.getCaseType().getTypeCode());
        }

        return dto;
    }
}
