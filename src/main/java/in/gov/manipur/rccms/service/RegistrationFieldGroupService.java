package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.CreateRegistrationFieldGroupDTO;
import in.gov.manipur.rccms.dto.RegistrationFieldGroupDTO;
import in.gov.manipur.rccms.entity.RegistrationFieldGroup;
import in.gov.manipur.rccms.entity.RegistrationFormField;
import in.gov.manipur.rccms.repository.RegistrationFieldGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Registration Field Group Service
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationFieldGroupService {

    private final RegistrationFieldGroupRepository groupRepository;

    @Transactional(readOnly = true)
    public List<RegistrationFieldGroupDTO> getActiveGroups(RegistrationFormField.RegistrationType type) {
        List<RegistrationFieldGroup> groups = groupRepository.findActiveGroupsByType(type);
        return toDtos(groups);
    }

    @Transactional(readOnly = true)
    public List<RegistrationFieldGroupDTO> getAllGroups(RegistrationFormField.RegistrationType type) {
        List<RegistrationFieldGroup> groups = groupRepository.findAllGroupsByType(type);
        return toDtos(groups);
    }

    @Transactional(readOnly = true)
    public RegistrationFieldGroupDTO getGroupById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }
        RegistrationFieldGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registration field group not found: " + id));
        return toDto(group);
    }

    public RegistrationFieldGroupDTO createGroup(CreateRegistrationFieldGroupDTO dto) {
        if (groupRepository.findByRegistrationTypeAndGroupCode(dto.getRegistrationType(), dto.getGroupCode()).isPresent()) {
            throw new RuntimeException("Group already exists: " + dto.getGroupCode());
        }
        RegistrationFieldGroup group = new RegistrationFieldGroup();
        applyDto(group, dto);
        RegistrationFieldGroup saved = groupRepository.save(group);
        return toDto(saved);
    }

    public RegistrationFieldGroupDTO updateGroup(Long groupId, CreateRegistrationFieldGroupDTO dto) {
        if (groupId == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }
        RegistrationFieldGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Registration field group not found: " + groupId));

        if (!group.getGroupCode().equals(dto.getGroupCode())) {
            if (groupRepository.findByRegistrationTypeAndGroupCode(dto.getRegistrationType(), dto.getGroupCode()).isPresent()) {
                throw new RuntimeException("Group already exists: " + dto.getGroupCode());
            }
        }

        applyDto(group, dto);
        RegistrationFieldGroup saved = groupRepository.save(group);
        return toDto(saved);
    }

    public void deleteGroup(Long groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }
        RegistrationFieldGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Registration field group not found: " + groupId));
        groupRepository.delete(java.util.Objects.requireNonNull(group));
    }

    @Transactional(readOnly = true)
    public boolean existsActiveGroup(RegistrationFormField.RegistrationType type, String groupCode) {
        if (groupCode == null || groupCode.trim().isEmpty()) {
            return true;
        }
        return groupRepository.findByRegistrationTypeAndGroupCode(type, groupCode).isPresent();
    }

    private List<RegistrationFieldGroupDTO> toDtos(List<RegistrationFieldGroup> groups) {
        List<RegistrationFieldGroupDTO> dtos = new ArrayList<>();
        for (RegistrationFieldGroup group : groups) {
            dtos.add(toDto(group));
        }
        return dtos;
    }

    private RegistrationFieldGroupDTO toDto(RegistrationFieldGroup group) {
        return new RegistrationFieldGroupDTO(
                group.getId(),
                group.getRegistrationType(),
                group.getGroupCode(),
                group.getGroupLabel(),
                group.getDescription(),
                group.getDisplayOrder(),
                group.getIsActive()
        );
    }

    private void applyDto(RegistrationFieldGroup group, CreateRegistrationFieldGroupDTO dto) {
        group.setRegistrationType(dto.getRegistrationType());
        group.setGroupCode(dto.getGroupCode());
        group.setGroupLabel(dto.getGroupLabel());
        group.setDescription(dto.getDescription());
        group.setDisplayOrder(dto.getDisplayOrder());
        group.setIsActive(dto.getIsActive());
    }
}
