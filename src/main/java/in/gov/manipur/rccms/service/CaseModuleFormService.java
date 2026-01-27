package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.*;
import in.gov.manipur.rccms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for case module forms (hearing, notice, ordersheet, judgement)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CaseModuleFormService {

    private final CaseModuleFormFieldRepository fieldRepository;
    private final CaseModuleFormSubmissionRepository submissionRepository;
    private final CaseRepository caseRepository;
    private final CaseNatureRepository caseNatureRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final CaseWorkflowInstanceRepository workflowInstanceRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ModuleFormSchemaDTO getFormSchema(Long caseNatureId, Long caseTypeId, ModuleType moduleType) {
        if (caseNatureId == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("Module type cannot be null");
        }
        CaseNature caseNature = caseNatureRepository.findById(caseNatureId)
                .orElseThrow(() -> new RuntimeException("Case nature not found: " + caseNatureId));

        CaseType caseType = null;
        if (caseTypeId != null) {
            caseType = caseTypeRepository.findById(caseTypeId).orElse(null);
        }

        List<CaseModuleFormFieldDefinition> fields = new ArrayList<>();
        if (caseTypeId != null) {
            fields = fieldRepository.findActiveFieldsByCaseType(caseNatureId, caseTypeId, moduleType);
        }
        if (fields.isEmpty()) {
            fields = fieldRepository.findActiveFields(caseNatureId, moduleType);
        }
        List<ModuleFormFieldDTO> fieldDTOs = fields.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ModuleFormSchemaDTO.builder()
                .caseNatureId(caseNatureId)
                .caseNatureCode(caseNature.getCode())
                .caseNatureName(caseNature.getName())
                .caseTypeId(caseTypeId)
                .caseTypeCode(caseType != null ? caseType.getTypeCode() : null)
                .caseTypeName(caseType != null ? caseType.getTypeName() : null)
                .moduleType(moduleType)
                .fields(fieldDTOs)
                .totalFields(fieldDTOs.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ModuleFormFieldDTO> getAllFields(Long caseNatureId, Long caseTypeId, ModuleType moduleType) {
        if (caseNatureId == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("Module type cannot be null");
        }
        List<CaseModuleFormFieldDefinition> fields;
        if (caseTypeId != null) {
            fields = fieldRepository.findAllFieldsByCaseType(caseNatureId, caseTypeId, moduleType);
        } else {
            fields = fieldRepository.findAllFields(caseNatureId, moduleType);
        }
        return fields.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ModuleFormFieldDTO createField(CreateModuleFormFieldDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("CreateModuleFormFieldDTO cannot be null");
        }
        Long caseNatureId = dto.getCaseNatureId();
        if (caseNatureId == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        Long caseTypeId = dto.getCaseTypeId();
        CaseNature caseNature = caseNatureRepository.findById(caseNatureId)
                .orElseThrow(() -> new RuntimeException("Case nature not found: " + caseNatureId));
        CaseType caseType = null;
        if (caseTypeId != null) {
            caseType = caseTypeRepository.findById(caseTypeId)
                    .orElseThrow(() -> new RuntimeException("Case type not found: " + caseTypeId));
        }

        if (fieldRepository.existsByCaseNatureIdAndCaseTypeIdAndModuleTypeAndFieldName(
                caseNatureId, caseTypeId, dto.getModuleType(), dto.getFieldName())) {
            throw new RuntimeException("Field name already exists for this case nature and module type");
        }

        CaseModuleFormFieldDefinition field = new CaseModuleFormFieldDefinition();
        field.setCaseNature(caseNature);
        field.setCaseNatureId(caseNature.getId());
        field.setCaseType(caseType);
        field.setCaseTypeId(caseTypeId);
        field.setModuleType(dto.getModuleType());
        field.setFieldName(dto.getFieldName());
        field.setFieldLabel(dto.getFieldLabel());
        field.setFieldType(dto.getFieldType());
        field.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : false);
        field.setValidationRules(dto.getValidationRules());
        field.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        field.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        field.setDefaultValue(dto.getDefaultValue());
        field.setFieldOptions(dto.getFieldOptions());
        field.setPlaceholder(dto.getPlaceholder());
        field.setHelpText(dto.getHelpText());
        field.setDataSource(dto.getDataSource());
        field.setDependsOnField(dto.getDependsOnField());
        field.setDependencyCondition(dto.getDependencyCondition());
        field.setConditionalLogic(dto.getConditionalLogic());

        return toDto(fieldRepository.save(field));
    }

    public ModuleFormFieldDTO updateField(Long fieldId, UpdateModuleFormFieldDTO dto) {
        if (fieldId == null) {
            throw new IllegalArgumentException("Field ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("UpdateModuleFormFieldDTO cannot be null");
        }
        CaseModuleFormFieldDefinition field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Module form field not found: " + fieldId));

        Long caseTypeId = dto.getCaseTypeId();
        if (caseTypeId != null) {
            CaseType caseType = caseTypeRepository.findById(caseTypeId)
                    .orElseThrow(() -> new RuntimeException("Case type not found: " + caseTypeId));
            field.setCaseType(caseType);
            field.setCaseTypeId(caseTypeId);
        } else {
            field.setCaseType(null);
            field.setCaseTypeId(null);
        }
        field.setFieldName(dto.getFieldName());
        field.setFieldLabel(dto.getFieldLabel());
        field.setFieldType(dto.getFieldType());
        field.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : field.getIsRequired());
        field.setValidationRules(dto.getValidationRules());
        field.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : field.getDisplayOrder());
        field.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : field.getIsActive());
        field.setDefaultValue(dto.getDefaultValue());
        field.setFieldOptions(dto.getFieldOptions());
        field.setPlaceholder(dto.getPlaceholder());
        field.setHelpText(dto.getHelpText());
        field.setDataSource(dto.getDataSource());
        field.setDependsOnField(dto.getDependsOnField());
        field.setDependencyCondition(dto.getDependencyCondition());
        field.setConditionalLogic(dto.getConditionalLogic());

        return toDto(fieldRepository.save(field));
    }

    public void deleteField(Long fieldId) {
        if (fieldId == null) {
            throw new IllegalArgumentException("Field ID cannot be null");
        }
        CaseModuleFormFieldDefinition field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Module form field not found: " + fieldId));
        if (field == null) {
            throw new RuntimeException("Module form field not found: " + fieldId);
        }
        fieldRepository.delete(field);
    }

    public ModuleFormSubmissionDTO submitForm(Long caseId, ModuleType moduleType, Long officerId, CreateModuleFormSubmissionDTO dto) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("Module type cannot be null");
        }
        if (officerId == null) {
            throw new IllegalArgumentException("Officer ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("CreateModuleFormSubmissionDTO cannot be null");
        }
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        CaseModuleFormSubmission submission = new CaseModuleFormSubmission();
        submission.setCaseEntity(caseEntity);
        submission.setCaseId(caseEntity.getId());
        submission.setCaseNature(caseEntity.getCaseNature());
        submission.setCaseNatureId(caseEntity.getCaseNatureId());
        submission.setModuleType(moduleType);
        submission.setFormData(dto.getFormData());
        submission.setRemarks(dto.getRemarks());
        submission.setSubmittedByOfficerId(officerId);

        CaseModuleFormSubmission saved = submissionRepository.save(submission);

        // Update workflow data flag for checklist
        updateWorkflowFlag(caseId, moduleType.name() + "_SUBMITTED", true);

        return toSubmissionDto(saved);
    }

    @Transactional(readOnly = true)
    public Optional<ModuleFormSubmissionDTO> getLatestSubmission(Long caseId, ModuleType moduleType) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("Module type cannot be null");
        }
        return submissionRepository.findTopByCaseIdAndModuleTypeOrderBySubmittedAtDesc(caseId, moduleType)
                .map(this::toSubmissionDto);
    }

    private void updateWorkflowFlag(Long caseId, String key, boolean value) {
        workflowInstanceRepository.findByCaseId(caseId).ifPresent(instance -> {
            Map<String, Object> data = parseJsonMap(instance.getWorkflowData());
            data.put(key, value);
            try {
                instance.setWorkflowData(objectMapper.writeValueAsString(data));
                workflowInstanceRepository.save(instance);
            } catch (Exception e) {
                log.error("Failed to update workflow data for case {}: {}", caseId, e.getMessage());
            }
        });
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Invalid workflow data JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private ModuleFormFieldDTO toDto(CaseModuleFormFieldDefinition field) {
        ModuleFormFieldDTO dto = new ModuleFormFieldDTO();
        dto.setId(field.getId());
        dto.setCaseNatureId(field.getCaseNatureId());
        dto.setCaseTypeId(field.getCaseTypeId());
        dto.setModuleType(field.getModuleType());
        dto.setFieldName(field.getFieldName());
        dto.setFieldLabel(field.getFieldLabel());
        dto.setFieldType(field.getFieldType());
        dto.setIsRequired(field.getIsRequired());
        dto.setValidationRules(field.getValidationRules());
        dto.setDisplayOrder(field.getDisplayOrder());
        dto.setIsActive(field.getIsActive());
        dto.setDefaultValue(field.getDefaultValue());
        dto.setFieldOptions(field.getFieldOptions());
        dto.setPlaceholder(field.getPlaceholder());
        dto.setHelpText(field.getHelpText());
        dto.setDataSource(field.getDataSource());
        dto.setDependsOnField(field.getDependsOnField());
        dto.setDependencyCondition(field.getDependencyCondition());
        dto.setConditionalLogic(field.getConditionalLogic());
        dto.setCreatedAt(field.getCreatedAt());
        dto.setUpdatedAt(field.getUpdatedAt());
        if (field.getCaseNature() != null) {
            dto.setCaseNatureCode(field.getCaseNature().getCode());
            dto.setCaseNatureName(field.getCaseNature().getName());
        }
        if (field.getCaseType() != null) {
            dto.setCaseTypeCode(field.getCaseType().getTypeCode());
            dto.setCaseTypeName(field.getCaseType().getTypeName());
        }
        return dto;
    }

    private ModuleFormSubmissionDTO toSubmissionDto(CaseModuleFormSubmission submission) {
        ModuleFormSubmissionDTO dto = new ModuleFormSubmissionDTO();
        dto.setId(submission.getId());
        dto.setCaseId(submission.getCaseId());
        dto.setCaseNatureId(submission.getCaseNatureId());
        dto.setModuleType(submission.getModuleType());
        dto.setFormData(submission.getFormData());
        dto.setSubmittedByOfficerId(submission.getSubmittedByOfficerId());
        dto.setSubmittedAt(submission.getSubmittedAt());
        dto.setRemarks(submission.getRemarks());
        return dto;
    }
}

