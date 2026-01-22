package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.CreateRegistrationFormFieldDTO;
import in.gov.manipur.rccms.dto.RegistrationFormFieldDTO;
import in.gov.manipur.rccms.dto.RegistrationFormSchemaDTO;
import in.gov.manipur.rccms.entity.RegistrationFormField;
import in.gov.manipur.rccms.repository.RegistrationFormFieldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Registration Form Service
 * Handles dynamic registration form schema for Citizen and Lawyer
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationFormService {

    private final RegistrationFormFieldRepository fieldRepository;
    private final RegistrationFieldGroupService registrationFieldGroupService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public RegistrationFormSchemaDTO getSchema(RegistrationFormField.RegistrationType type) {
        List<RegistrationFormField> fields = fieldRepository.findActiveFieldsByType(type);
        List<RegistrationFormFieldDTO> dtos = new ArrayList<>();
        for (RegistrationFormField field : fields) {
            dtos.add(convertToDTO(field));
        }
        return new RegistrationFormSchemaDTO(type, dtos);
    }

    @Transactional(readOnly = true)
    public List<RegistrationFormFieldDTO> getAllFields(RegistrationFormField.RegistrationType type) {
        List<RegistrationFormField> fields = fieldRepository.findAllFieldsByType(type);
        List<RegistrationFormFieldDTO> dtos = new ArrayList<>();
        for (RegistrationFormField field : fields) {
            dtos.add(convertToDTO(field));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<String> getFieldGroups(RegistrationFormField.RegistrationType type) {
        return fieldRepository.findDistinctFieldGroups(type);
    }

    @Transactional(readOnly = true)
    public RegistrationFormFieldDTO getFieldById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Field ID cannot be null");
        }
        RegistrationFormField field = fieldRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registration form field not found: " + id));
        return convertToDTO(field);
    }

    public RegistrationFormFieldDTO createField(CreateRegistrationFormFieldDTO dto) {
        if (fieldRepository.findByRegistrationTypeAndFieldName(dto.getRegistrationType(), dto.getFieldName()).isPresent()) {
            throw new RuntimeException("Field already exists: " + dto.getFieldName());
        }
        if (!registrationFieldGroupService.existsActiveGroup(dto.getRegistrationType(), dto.getFieldGroup())) {
            throw new RuntimeException("Invalid field group: " + dto.getFieldGroup());
        }

        RegistrationFormField field = new RegistrationFormField();
        applyDto(field, dto);
        RegistrationFormField saved = fieldRepository.save(field);
        return convertToDTO(saved);
    }

    public RegistrationFormFieldDTO updateField(Long fieldId, CreateRegistrationFormFieldDTO dto) {
        if (fieldId == null) {
            throw new IllegalArgumentException("Field ID cannot be null");
        }
        RegistrationFormField field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Registration form field not found: " + fieldId));

        if (!field.getFieldName().equals(dto.getFieldName())) {
            if (fieldRepository.findByRegistrationTypeAndFieldName(dto.getRegistrationType(), dto.getFieldName()).isPresent()) {
                throw new RuntimeException("Field already exists: " + dto.getFieldName());
            }
        }
        if (!registrationFieldGroupService.existsActiveGroup(dto.getRegistrationType(), dto.getFieldGroup())) {
            throw new RuntimeException("Invalid field group: " + dto.getFieldGroup());
        }

        applyDto(field, dto);
        RegistrationFormField saved = fieldRepository.save(field);
        return convertToDTO(saved);
    }

    public void deleteField(Long fieldId) {
        if (fieldId == null) {
            throw new IllegalArgumentException("Field ID cannot be null");
        }
        RegistrationFormField field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Registration form field not found: " + fieldId));
        fieldRepository.delete(java.util.Objects.requireNonNull(field));
    }

    /**
     * Validate registration data against schema (required fields + basic rules)
     */
    @Transactional(readOnly = true)
    public void validateRegistrationData(RegistrationFormField.RegistrationType type, Map<String, Object> data) {
        List<RegistrationFormField> fields = fieldRepository.findActiveFieldsByType(type);
        for (RegistrationFormField field : fields) {
            Object value = data.get(field.getFieldName());

            if (Boolean.TRUE.equals(field.getIsRequired())) {
                if (value == null || value.toString().trim().isEmpty()) {
                    throw new IllegalArgumentException("Validation failed: " + field.getFieldName() + " is required");
                }
            }

            if (value != null && field.getValidationRules() != null && !field.getValidationRules().trim().isEmpty()) {
                validateRules(field.getFieldName(), value, field.getValidationRules());
            }
        }
    }

    private void validateRules(String fieldName, Object value, String rulesJson) {
        try {
            Map<String, Object> rules = objectMapper.readValue(rulesJson, new TypeReference<Map<String, Object>>() {});
            String stringValue = value.toString();

            if (rules.containsKey("minLength")) {
                int min = Integer.parseInt(rules.get("minLength").toString());
                if (stringValue.length() < min) {
                    throw new IllegalArgumentException("Validation failed: " + fieldName + " minLength " + min);
                }
            }
            if (rules.containsKey("maxLength")) {
                int max = Integer.parseInt(rules.get("maxLength").toString());
                if (stringValue.length() > max) {
                    throw new IllegalArgumentException("Validation failed: " + fieldName + " maxLength " + max);
                }
            }
            if (rules.containsKey("pattern")) {
                String pattern = rules.get("pattern").toString();
                if (!stringValue.matches(pattern)) {
                    throw new IllegalArgumentException("Validation failed: " + fieldName + " pattern");
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid validation rules for field: " + fieldName);
        }
    }

    private void applyDto(RegistrationFormField field, CreateRegistrationFormFieldDTO dto) {
        field.setRegistrationType(dto.getRegistrationType());
        field.setFieldName(dto.getFieldName());
        field.setFieldLabel(dto.getFieldLabel());
        field.setFieldType(dto.getFieldType());
        field.setIsRequired(dto.getIsRequired());
        field.setValidationRules(dto.getValidationRules());
        field.setDisplayOrder(dto.getDisplayOrder());
        field.setIsActive(dto.getIsActive());
        field.setDefaultValue(dto.getDefaultValue());
        field.setFieldOptions(dto.getFieldOptions());
        field.setDataSource(dto.getDataSource());
        field.setPlaceholder(dto.getPlaceholder());
        field.setHelpText(dto.getHelpText());
        field.setFieldGroup(dto.getFieldGroup());
        field.setConditionalLogic(dto.getConditionalLogic());
    }

    private RegistrationFormFieldDTO convertToDTO(RegistrationFormField field) {
        return new RegistrationFormFieldDTO(
                field.getId(),
                field.getRegistrationType(),
                field.getFieldName(),
                field.getFieldLabel(),
                field.getFieldType(),
                field.getIsRequired(),
                field.getValidationRules(),
                field.getDisplayOrder(),
                field.getIsActive(),
                field.getDefaultValue(),
                field.getFieldOptions(),
                field.getDataSource(),
                field.getPlaceholder(),
                field.getHelpText(),
                field.getFieldGroup(),
                field.getConditionalLogic()
        );
    }
}
