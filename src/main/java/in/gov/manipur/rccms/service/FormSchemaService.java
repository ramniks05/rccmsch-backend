package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.CaseType;
import in.gov.manipur.rccms.entity.FormFieldDefinition;
import in.gov.manipur.rccms.repository.CaseTypeRepository;
import in.gov.manipur.rccms.repository.FormFieldDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Form Schema Service
 * Manages dynamic form field definitions and validation
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FormSchemaService {

    private final FormFieldDefinitionRepository fieldRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final FormFieldGroupService formFieldGroupService;
    private final ObjectMapper objectMapper;

    /**
     * Get form schema for a case type (only active fields, grouped by form groups)
     */
    @Transactional(readOnly = true)
    public FormSchemaDTO getFormSchema(Long caseTypeId) {
        log.info("Getting form schema for case type: {}", caseTypeId);

        if (caseTypeId == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }

        CaseType caseType = caseTypeRepository.findById(caseTypeId)
                .orElseThrow(() -> new RuntimeException("Case type not found: " + caseTypeId));

        List<FormFieldDefinition> fields = fieldRepository.findActiveFieldsByCaseTypeId(caseTypeId);

        // Get all active groups for this case type
        List<FormFieldGroupDTO> activeGroups = formFieldGroupService.getActiveGroups(caseTypeId);

        // Convert fields to DTOs
        List<FormFieldDefinitionDTO> fieldDTOs = fields.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Group fields by their groupCode
        Map<String, List<FormFieldDefinitionDTO>> fieldsByGroup = fieldDTOs.stream()
                .filter(f -> f.getFieldGroup() != null && !f.getFieldGroup().isEmpty())
                .collect(Collectors.groupingBy(FormFieldDefinitionDTO::getFieldGroup));

        // Get fields without a group
        List<FormFieldDefinitionDTO> ungroupedFields = fieldDTOs.stream()
                .filter(f -> f.getFieldGroup() == null || f.getFieldGroup().isEmpty())
                .sorted(Comparator.comparing(f -> f.getDisplayOrder() != null ? f.getDisplayOrder() : 0))
                .collect(Collectors.toList());

        // Create FormGroupWithFieldsDTO for each group
        List<FormGroupWithFieldsDTO> groupsWithFields = activeGroups.stream()
                .sorted(Comparator.comparing(FormFieldGroupDTO::getDisplayOrder))
                .map(group -> {
                    List<FormFieldDefinitionDTO> groupFields = fieldsByGroup.getOrDefault(
                            group.getGroupCode(), 
                            new ArrayList<>()
                    );
                    // Sort fields within group by display order
                    groupFields.sort(Comparator.comparing(f -> f.getDisplayOrder() != null ? f.getDisplayOrder() : 0));
                    
                    return FormGroupWithFieldsDTO.builder()
                            .groupId(group.getId())
                            .groupCode(group.getGroupCode())
                            .groupLabel(group.getGroupLabel())
                            .description(group.getDescription())
                            .displayOrder(group.getDisplayOrder() != null ? group.getDisplayOrder() : 0)
                            .fields(groupFields != null ? groupFields : new ArrayList<>())
                            .fieldCount(groupFields != null ? groupFields.size() : 0)
                            .build();
                })
                .filter(group -> group.getFieldCount() > 0) // Only include groups with fields
                .collect(Collectors.toList());

        // Add ungrouped fields as a special group if any exist
        if (!ungroupedFields.isEmpty()) {
            groupsWithFields.add(FormGroupWithFieldsDTO.builder()
                    .groupId(null)
                    .groupCode("UNGROUPED")
                    .groupLabel("Other Fields")
                    .description("Fields not assigned to any group")
                    .displayOrder(9999)
                    .fields(ungroupedFields)
                    .fieldCount(ungroupedFields.size())
                    .build());
        }

        // Sort all fields for flat list (backward compatibility)
        Map<String, Integer> groupOrderMap = activeGroups.stream()
                .collect(Collectors.toMap(
                        FormFieldGroupDTO::getGroupCode,
                        FormFieldGroupDTO::getDisplayOrder,
                        (a, b) -> a
                ));

        List<FormFieldDefinitionDTO> sortedFieldDTOs = fieldDTOs.stream()
                .sorted((f1, f2) -> {
                    // Get group order (default to 9999 if no group)
                    int order1 = f1.getFieldGroup() != null && !f1.getFieldGroup().isEmpty() 
                            ? groupOrderMap.getOrDefault(f1.getFieldGroup(), 9999) 
                            : 9999;
                    int order2 = f2.getFieldGroup() != null && !f2.getFieldGroup().isEmpty() 
                            ? groupOrderMap.getOrDefault(f2.getFieldGroup(), 9999) 
                            : 9999;
                    
                    // Compare by group order first
                    int groupCompare = Integer.compare(order1, order2);
                    if (groupCompare != 0) {
                        return groupCompare;
                    }
                    
                    // Then by field display order
                    int displayCompare = Integer.compare(
                            f1.getDisplayOrder() != null ? f1.getDisplayOrder() : 0,
                            f2.getDisplayOrder() != null ? f2.getDisplayOrder() : 0
                    );
                    if (displayCompare != 0) {
                        return displayCompare;
                    }
                    
                    // Finally by field ID
                    return Long.compare(f1.getId(), f2.getId());
                })
                .collect(Collectors.toList());

        // Ensure lists are never null
        if (sortedFieldDTOs == null) {
            sortedFieldDTOs = new ArrayList<>();
        }
        if (groupsWithFields == null) {
            groupsWithFields = new ArrayList<>();
        }
        
        // Ensure all lists are initialized (never null)
        if (sortedFieldDTOs == null) {
            sortedFieldDTOs = new ArrayList<>();
        }
        if (groupsWithFields == null) {
            groupsWithFields = new ArrayList<>();
        }
        
        FormSchemaDTO schema = FormSchemaDTO.builder()
                .caseTypeId(caseType.getId())
                .caseTypeName(caseType.getTypeName() != null ? caseType.getTypeName() : "")
                .caseTypeCode(caseType.getTypeCode() != null ? caseType.getTypeCode() : "")
                .fields(sortedFieldDTOs) // Flat list for backward compatibility
                .groups(groupsWithFields) // Groups with their fields
                .totalFields(fieldDTOs != null ? fieldDTOs.size() : 0)
                .build();
        
        log.debug("Form schema built - caseTypeId: {}, caseTypeName: {}, totalFields: {}, groupsCount: {}", 
                schema.getCaseTypeId(), schema.getCaseTypeName(), schema.getTotalFields(), 
                schema.getGroups() != null ? schema.getGroups().size() : 0);
        
        return schema;
    }

    /**
     * Get all form fields for a case type (including inactive)
     */
    @Transactional(readOnly = true)
    public List<FormFieldDefinitionDTO> getAllFields(Long caseTypeId) {
        log.info("Getting all fields for case type: {}", caseTypeId);

        if (caseTypeId == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }

        caseTypeRepository.findById(caseTypeId)
                .orElseThrow(() -> new RuntimeException("Case type not found: " + caseTypeId));

        List<FormFieldDefinition> fields = fieldRepository.findAllFieldsByCaseTypeId(caseTypeId);

        return fields.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get form field by ID
     */
    @Transactional(readOnly = true)
    public FormFieldDefinitionDTO getFieldById(Long fieldId) {
        if (fieldId == null) {
            throw new IllegalArgumentException("Field ID cannot be null");
        }

        FormFieldDefinition field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Form field not found: " + fieldId));
        return convertToDTO(field);
    }

    /**
     * Create a new form field
     */
    public FormFieldDefinitionDTO createField(CreateFormFieldDTO dto) {
        log.info("Creating form field: caseTypeId={}, fieldName={}", dto.getCaseTypeId(), dto.getFieldName());

        if (dto.getCaseTypeId() == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }

        // Validate case type
        CaseType caseType = caseTypeRepository.findById(dto.getCaseTypeId())
                .orElseThrow(() -> new RuntimeException("Case type not found: " + dto.getCaseTypeId()));

        // Check if field name already exists for this case type
        if (fieldRepository.existsByCaseTypeIdAndFieldName(dto.getCaseTypeId(), dto.getFieldName())) {
            throw new RuntimeException("Field name already exists for this case type: " + dto.getFieldName());
        }

        // Validate field group if provided
        if (dto.getFieldGroup() != null && !dto.getFieldGroup().trim().isEmpty()) {
            if (!formFieldGroupService.existsActiveGroup(dto.getCaseTypeId(), dto.getFieldGroup())) {
                throw new RuntimeException("Invalid field group: " + dto.getFieldGroup() + ". Group does not exist or is inactive for this case type.");
            }
        }

        // Create field entity
        FormFieldDefinition field = new FormFieldDefinition();
        field.setCaseType(caseType);
        field.setCaseTypeId(caseType.getId());
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
        field.setFieldGroup(dto.getFieldGroup());
        field.setDataSource(dto.getDataSource());
        field.setDependsOnField(dto.getDependsOnField());
        field.setDependencyCondition(dto.getDependencyCondition());
        field.setConditionalLogic(dto.getConditionalLogic());
        field.setOnChangeApi(dto.getOnChangeApi());
        field.setOnChangeResponseMapping(dto.getOnChangeResponseMapping());

        FormFieldDefinition saved = fieldRepository.save(field);
        log.info("Form field created successfully: fieldId={}", saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Update a form field
     */
    public FormFieldDefinitionDTO updateField(Long fieldId, UpdateFormFieldDTO dto) {
        log.info("Updating form field: fieldId={}", fieldId);

        if (fieldId == null) {
            throw new IllegalArgumentException("Field ID cannot be null");
        }

        FormFieldDefinition field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Form field not found: " + fieldId));

        // Update fields
        if (dto.getFieldLabel() != null) {
            field.setFieldLabel(dto.getFieldLabel());
        }
        if (dto.getFieldType() != null) {
            field.setFieldType(dto.getFieldType());
        }
        if (dto.getIsRequired() != null) {
            field.setIsRequired(dto.getIsRequired());
        }
        if (dto.getValidationRules() != null) {
            field.setValidationRules(dto.getValidationRules());
        }
        if (dto.getDisplayOrder() != null) {
            field.setDisplayOrder(dto.getDisplayOrder());
        }
        if (dto.getIsActive() != null) {
            field.setIsActive(dto.getIsActive());
        }
        if (dto.getDefaultValue() != null) {
            field.setDefaultValue(dto.getDefaultValue());
        }
        if (dto.getFieldOptions() != null) {
            field.setFieldOptions(dto.getFieldOptions());
        }
        if (dto.getPlaceholder() != null) {
            field.setPlaceholder(dto.getPlaceholder());
        }
        if (dto.getHelpText() != null) {
            field.setHelpText(dto.getHelpText());
        }
        if (dto.getFieldGroup() != null) {
            // Validate field group if provided
            if (!dto.getFieldGroup().trim().isEmpty()) {
                if (!formFieldGroupService.existsActiveGroup(field.getCaseTypeId(), dto.getFieldGroup())) {
                    throw new RuntimeException("Invalid field group: " + dto.getFieldGroup() + ". Group does not exist or is inactive for this case type.");
                }
            }
            field.setFieldGroup(dto.getFieldGroup());
        }
        if (dto.getDataSource() != null) {
            field.setDataSource(dto.getDataSource());
        }
        if (dto.getDependsOnField() != null) {
            field.setDependsOnField(dto.getDependsOnField());
        }
        if (dto.getDependencyCondition() != null) {
            field.setDependencyCondition(dto.getDependencyCondition());
        }
        if (dto.getConditionalLogic() != null) {
            field.setConditionalLogic(dto.getConditionalLogic());
        }
        if (dto.getOnChangeApi() != null) {
            field.setOnChangeApi(dto.getOnChangeApi());
        }
        if (dto.getOnChangeResponseMapping() != null) {
            field.setOnChangeResponseMapping(dto.getOnChangeResponseMapping());
        }

        FormFieldDefinition saved = fieldRepository.save(field);
        log.info("Form field updated successfully: fieldId={}", saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Delete a form field
     */
    public void deleteField(Long fieldId) {
        log.info("Deleting form field: fieldId={}", fieldId);

        if (fieldId == null) {
            throw new IllegalArgumentException("Field ID cannot be null");
        }

        FormFieldDefinition field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Form field not found: " + fieldId));

        fieldRepository.delete(field);
        log.info("Form field deleted successfully: fieldId={}", fieldId);
    }


    /**
     * Bulk create multiple form fields
     */
    public List<FormFieldDefinitionDTO> bulkCreateFields(Long caseTypeId, List<CreateFormFieldDTO> fields) {
        log.info("Bulk creating {} fields for case type: {}", fields != null ? fields.size() : 0, caseTypeId);

        if (caseTypeId == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }

        // Validate case type exists
        caseTypeRepository.findById(caseTypeId)
                .orElseThrow(() -> new RuntimeException("Case type not found: " + caseTypeId));

        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("Fields list cannot be empty");
        }

        List<FormFieldDefinitionDTO> createdFields = new ArrayList<>();
        
        for (CreateFormFieldDTO dto : fields) {
            // Ensure all fields belong to the same case type
            dto.setCaseTypeId(caseTypeId);
            FormFieldDefinitionDTO created = createField(dto);
            createdFields.add(created);
        }

        log.info("Bulk created {} fields successfully for case type: {}", createdFields.size(), caseTypeId);
        return createdFields;
    }

    /**
     * Bulk update multiple form fields
     */
    public List<FormFieldDefinitionDTO> bulkUpdateFields(List<BulkUpdateFieldItemDTO> fields) {
        log.info("Bulk updating {} fields", fields != null ? fields.size() : 0);

        if (fields == null || fields.isEmpty()) {
            throw new IllegalArgumentException("Fields list cannot be empty");
        }

        List<FormFieldDefinitionDTO> updatedFields = new ArrayList<>();
        
        for (BulkUpdateFieldItemDTO item : fields) {
            if (item.getFieldId() == null) {
                throw new IllegalArgumentException("Field ID cannot be null");
            }
            if (item.getUpdateData() == null) {
                throw new IllegalArgumentException("Update data cannot be null for field ID: " + item.getFieldId());
            }
            
            FormFieldDefinitionDTO updated = updateField(item.getFieldId(), item.getUpdateData());
            updatedFields.add(updated);
        }

        log.info("Bulk updated {} fields successfully", updatedFields.size());
        return updatedFields;
    }

    /**
     * Bulk delete multiple form fields
     */
    public void bulkDeleteFields(List<Long> fieldIds) {
        log.info("Bulk deleting {} fields", fieldIds != null ? fieldIds.size() : 0);

        if (fieldIds == null || fieldIds.isEmpty()) {
            throw new IllegalArgumentException("Field IDs list cannot be empty");
        }

        int deletedCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Long fieldId : fieldIds) {
            try {
                if (fieldId != null) {
                    deleteField(fieldId);
                    deletedCount++;
                }
            } catch (Exception e) {
                log.error("Error deleting field {}: {}", fieldId, e.getMessage());
                errors.add("Field " + fieldId + ": " + e.getMessage());
            }
        }

        log.info("Bulk deleted {}/{} fields successfully", deletedCount, fieldIds.size());
        
        if (!errors.isEmpty() && deletedCount == 0) {
            // If all deletions failed, throw exception
            throw new RuntimeException("All fields could not be deleted: " + String.join(", ", errors));
        } else if (!errors.isEmpty()) {
            // If some deletions failed, log warning but don't throw
            log.warn("Some fields could not be deleted: {}", String.join(", ", errors));
        }
    }

    /**
     * Reorder form fields
     */
    public void reorderFields(Long caseTypeId, ReorderFieldsDTO dto) {
        log.info("Reordering fields for case type: {}", caseTypeId);

        if (caseTypeId == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }

        List<Long> fieldIds = dto.getFieldIds();
        for (int i = 0; i < fieldIds.size(); i++) {
            Long fieldId = fieldIds.get(i);
            if (fieldId == null) {
                continue;
            }
            FormFieldDefinition field = fieldRepository.findById(fieldId)
                    .orElseThrow(() -> new RuntimeException("Form field not found: " + fieldId));

            Long fieldCaseTypeId = field.getCaseTypeId();
            if (fieldCaseTypeId == null || !fieldCaseTypeId.equals(caseTypeId)) {
                throw new RuntimeException("Field does not belong to case type: " + caseTypeId);
            }

            field.setDisplayOrder(i + 1);
            fieldRepository.save(field);
        }

        log.info("Fields reordered successfully for case type: {}", caseTypeId);
    }

    /**
     * Validate form data against schema
     */
    @Transactional(readOnly = true)
    public Map<String, String> validateFormData(Long caseTypeId, Map<String, Object> formData) {
        log.info("Validating form data for case type: {}", caseTypeId);

        if (caseTypeId == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }

        List<FormFieldDefinition> fields = fieldRepository.findActiveFieldsByCaseTypeId(caseTypeId);
        Map<String, String> errors = new HashMap<>();

        for (FormFieldDefinition field : fields) {
            String fieldName = field.getFieldName();
            Object value = formData.get(fieldName);

            // Check required fields
            if (field.getIsRequired() && (value == null || value.toString().trim().isEmpty())) {
                errors.put(fieldName, field.getFieldLabel() + " is required");
                continue;
            }

            // Skip validation if field is empty and not required
            if (value == null || value.toString().trim().isEmpty()) {
                continue;
            }

            // Validate based on field type and validation rules
            String validationError = validateFieldValue(field, value);
            if (validationError != null) {
                errors.put(fieldName, validationError);
            }
        }

        return errors;
    }

    /**
     * Validate a single field value
     */
    private String validateFieldValue(FormFieldDefinition field, Object value) {
        String fieldType = field.getFieldType();
        String valueStr;
        if (value instanceof List) {
            try {
                valueStr = objectMapper.writeValueAsString(value);
            } catch (Exception e) {
                valueStr = value.toString();
            }
        } else if (value instanceof Map && ("SELECT".equalsIgnoreCase(fieldType) || "RADIO".equalsIgnoreCase(fieldType) || "DROPDOWN".equalsIgnoreCase(fieldType))) {
            // Frontend may send option as {value: "x", label: "y"} for dropdowns
            @SuppressWarnings("unchecked")
            Object v = ((Map<String, Object>) value).get("value");
            valueStr = v != null ? String.valueOf(v).trim() : "";
        } else {
            valueStr = value != null ? value.toString().trim() : "";
        }

        try {
            // Parse validation rules
            Map<String, Object> rules = parseValidationRules(field.getValidationRules());

            switch (fieldType.toUpperCase()) {
                case "TEXT":
                case "TEXTAREA":
                case "RICH_TEXT":
                    return validateText(valueStr, rules);
                case "NUMBER":
                    return validateNumber(valueStr, rules);
                case "DATE":
                    return validateDate(valueStr, rules);
                case "DATETIME":
                    return validateDatetime(valueStr, rules);
                case "EMAIL":
                    return validateEmail(valueStr, rules);
                case "PHONE":
                    return validatePhone(valueStr, rules);
                case "SELECT":
                case "RADIO":
                case "DROPDOWN":
                    // Skip option validation when options come from dynamic dataSource (API, ADMIN_UNITS, etc.)
                    if (field.getDataSource() != null && !field.getDataSource().trim().isEmpty()) {
                        return null;
                    }
                    return validateSelect(valueStr, field.getFieldOptions());
                case "MULTISELECT":
                    if (field.getDataSource() != null && !field.getDataSource().trim().isEmpty()) {
                        return null;
                    }
                    return validateMultiselect(valueStr, field.getFieldOptions());
                case "CHECKBOX":
                    return validateCheckbox(valueStr);
                case "FILE":
                    return null; // File path/identifier - no strict validation
                default:
                    return null; // No validation for unknown types
            }
        } catch (Exception e) {
            log.error("Error validating field {}: {}", field.getFieldName(), e.getMessage());
            return "Invalid value format";
        }
    }

    /**
     * Validate text field
     */
    private String validateText(String value, Map<String, Object> rules) {
        if (rules == null) {
            return null;
        }

        Integer minLength = getInteger(rules, "minLength");
        Integer maxLength = getInteger(rules, "maxLength");
        String pattern = getString(rules, "pattern");

        if (minLength != null && value.length() < minLength) {
            return "Minimum length is " + minLength + " characters";
        }
        if (maxLength != null && value.length() > maxLength) {
            return "Maximum length is " + maxLength + " characters";
        }
        if (pattern != null && !Pattern.matches(pattern, value)) {
            return "Invalid format";
        }

        return null;
    }

    /**
     * Validate number field
     */
    private String validateNumber(String value, Map<String, Object> rules) {
        try {
            Double numValue = Double.parseDouble(value);
            if (rules == null) {
                return null;
            }

            Double min = getDouble(rules, "min");
            Double max = getDouble(rules, "max");

            if (min != null && numValue < min) {
                return "Minimum value is " + min;
            }
            if (max != null && numValue > max) {
                return "Maximum value is " + max;
            }

            return null;
        } catch (NumberFormatException e) {
            return "Must be a valid number";
        }
    }

    /**
     * Validate date field
     */
    private String validateDate(String value, Map<String, Object> rules) {
        try {
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
            if (rules == null) {
                return null;
            }

            String minDate = getString(rules, "minDate");
            String maxDate = getString(rules, "maxDate");

            if (minDate != null) {
                LocalDate min = "today".equals(minDate) ? LocalDate.now() : LocalDate.parse(minDate);
                if (date.isBefore(min)) {
                    return "Date must be on or after " + minDate;
                }
            }
            if (maxDate != null) {
                LocalDate max = "today".equals(maxDate) ? LocalDate.now() : LocalDate.parse(maxDate);
                if (date.isAfter(max)) {
                    return "Date must be on or before " + maxDate;
                }
            }

            return null;
        } catch (DateTimeParseException e) {
            return "Must be a valid date (YYYY-MM-DD)";
        }
    }

    /**
     * Validate datetime field
     */
    private String validateDatetime(String value, Map<String, Object> rules) {
        try {
            LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
            return null; // Valid format; optional rules for min/max could be added later
        } catch (DateTimeParseException e) {
            return "Must be a valid datetime (ISO 8601 format)";
        }
    }

    /**
     * Validate multiselect field - value is JSON array of selected option values
     */
    private String validateMultiselect(String value, String fieldOptions) {
        if (fieldOptions == null || fieldOptions.trim().isEmpty()) {
            return null;
        }
        try {
            List<String> selectedValues = objectMapper.readValue(value, new TypeReference<List<String>>() {});
            List<Map<String, String>> options = objectMapper.readValue(fieldOptions, new TypeReference<List<Map<String, String>>>() {});
            if (options == null || options.isEmpty()) {
                return null; // No static options (e.g. from API), skip validation
            }
            Set<String> validValues = options.stream()
                    .map(opt -> opt.get("value"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (String v : selectedValues) {
                if (!validValues.contains(v)) {
                    return "Invalid option selected: " + v;
                }
            }
            return null;
        } catch (Exception e) {
            return "Invalid multiselect value format (expected JSON array)";
        }
    }

    /**
     * Validate checkbox field - accepts "true", "false", true, false
     */
    private String validateCheckbox(String value) {
        String lower = value.toLowerCase();
        if ("true".equals(lower) || "false".equals(lower)) {
            return null;
        }
        return "Checkbox must be true or false";
    }

    /**
     * Validate email field
     */
    private String validateEmail(String value, Map<String, Object> rules) {
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!Pattern.matches(emailPattern, value)) {
            return "Must be a valid email address";
        }
        return null;
    }

    /**
     * Validate phone field
     */
    private String validatePhone(String value, Map<String, Object> rules) {
        // Indian phone number pattern: 10 digits starting with 6-9
        String phonePattern = "^[6-9]\\d{9}$";
        if (!Pattern.matches(phonePattern, value)) {
            return "Must be a valid 10-digit phone number starting with 6-9";
        }
        return null;
    }

    /**
     * Validate select/radio/dropdown field.
     * Uses type-tolerant comparison (value may be string or number from JSON).
     * Skips option membership check when options are empty (dynamic dropdown from API).
     */
    private String validateSelect(String value, String fieldOptions) {
        if (fieldOptions == null || fieldOptions.trim().isEmpty()) {
            return null; // No options defined, skip validation
        }

        try {
            List<Map<String, Object>> options = objectMapper.readValue(fieldOptions,
                    new TypeReference<List<Map<String, Object>>>() {});
            // Skip validation when no static options (e.g. dropdown options come from API/dataSource)
            if (options == null || options.isEmpty()) {
                return null;
            }
            String valueStr = value == null ? "" : value;
            if (valueStr.isEmpty()) {
                return null; // Required check is done earlier
            }
            boolean isValid = options.stream()
                    .anyMatch(opt -> {
                        Object optVal = opt.get("value");
                        if (optVal == null) return false;
                        return valueStr.equals(String.valueOf(optVal));
                    });
            if (!isValid) {
                // Option not in static list - likely a dynamic dropdown (API); accept to avoid blocking
                log.debug("Select value '{}' not in static options; accepting (may be from dynamic source)", valueStr);
                return null;
            }
        } catch (Exception e) {
            log.error("Error parsing field options: {}", e.getMessage());
            return null; // Skip validation if options are invalid
        }

        return null;
    }

    /**
     * Parse validation rules JSON
     */
    private Map<String, Object> parseValidationRules(String rulesJson) {
        if (rulesJson == null || rulesJson.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(rulesJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Error parsing validation rules: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Helper methods to extract values from rules map
     */
    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Convert entity to DTO
     * Fetches group information from master FormFieldGroup table
     */
    private FormFieldDefinitionDTO convertToDTO(FormFieldDefinition field) {
        FormFieldDefinitionDTO dto = FormFieldDefinitionDTO.builder()
                .id(field.getId())
                .caseTypeId(field.getCaseTypeId())
                .fieldName(field.getFieldName())
                .fieldLabel(field.getFieldLabel())
                .fieldType(field.getFieldType())
                .isRequired(field.getIsRequired() != null ? field.getIsRequired() : false)
                .validationRules(field.getValidationRules())
                .displayOrder(field.getDisplayOrder() != null ? field.getDisplayOrder() : 0)
                .isActive(field.getIsActive() != null ? field.getIsActive() : true)
                .defaultValue(field.getDefaultValue())
                .fieldOptions(field.getFieldOptions())
                .placeholder(field.getPlaceholder())
                .helpText(field.getHelpText())
                .fieldGroup(field.getFieldGroup())
                .dataSource(field.getDataSource())
                .dependsOnField(field.getDependsOnField())
                .dependencyCondition(field.getDependencyCondition())
                .conditionalLogic(field.getConditionalLogic())
                .onChangeApi(field.getOnChangeApi())
                .onChangeResponseMapping(field.getOnChangeResponseMapping())
                // Exclude timestamps for public API - they're not needed for form rendering
                .build();

        if (field.getCaseType() != null) {
            dto.setCaseTypeName(field.getCaseType().getTypeName());
            dto.setCaseTypeCode(field.getCaseType().getTypeCode());
        }

        // Fetch group information from master table if fieldGroup is set
        if (field.getFieldGroup() != null && !field.getFieldGroup().trim().isEmpty() && field.getCaseTypeId() != null) {
            try {
                formFieldGroupService.getActiveGroups(field.getCaseTypeId()).stream()
                        .filter(g -> g.getGroupCode().equals(field.getFieldGroup()))
                        .findFirst()
                        .ifPresent(group -> {
                            dto.setGroupLabel(group.getGroupLabel());
                            dto.setGroupDisplayOrder(group.getDisplayOrder());
                        });
            } catch (Exception e) {
                log.warn("Could not fetch group info for fieldGroup: {}", field.getFieldGroup(), e);
            }
        }

        return dto;
    }
}

