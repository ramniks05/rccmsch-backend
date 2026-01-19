package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.service.FormSchemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Form Schema Controller
 * Admin endpoints for managing dynamic form field definitions
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/form-schemas")
@RequiredArgsConstructor
public class FormSchemaController {

    private final FormSchemaService formSchemaService;

    /**
     * Get form schema for a case type (for frontend - only active fields)
     * Public endpoint - can be used by citizens to get form structure
     */
    @GetMapping("/case-types/{caseTypeId}")
    public ResponseEntity<ApiResponse<FormSchemaDTO>> getFormSchema(@PathVariable Long caseTypeId) {
        log.info("Getting form schema for case type: {}", caseTypeId);
        FormSchemaDTO schema = formSchemaService.getFormSchema(caseTypeId);
        return ResponseEntity.ok(ApiResponse.success("Form schema retrieved successfully", schema));
    }

    /**
     * Get all form fields for a case type (including inactive) - Admin only
     */
    @GetMapping("/case-types/{caseTypeId}/fields")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<FormFieldDefinitionDTO>>> getAllFields(@PathVariable Long caseTypeId) {
        log.info("Getting all fields for case type: {}", caseTypeId);
        List<FormFieldDefinitionDTO> fields = formSchemaService.getAllFields(caseTypeId);
        return ResponseEntity.ok(ApiResponse.success("Fields retrieved successfully", fields));
    }

    /**
     * Get form field by ID - Admin only
     */
    @GetMapping("/fields/{fieldId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<FormFieldDefinitionDTO>> getFieldById(@PathVariable Long fieldId) {
        log.info("Getting form field: {}", fieldId);
        FormFieldDefinitionDTO field = formSchemaService.getFieldById(fieldId);
        return ResponseEntity.ok(ApiResponse.success("Field retrieved successfully", field));
    }

    /**
     * Create a new form field - Admin only
     */
    @PostMapping("/fields")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<FormFieldDefinitionDTO>> createField(
            @Valid @RequestBody CreateFormFieldDTO dto) {
        log.info("Creating form field: caseTypeId={}, fieldName={}", dto.getCaseTypeId(), dto.getFieldName());
        FormFieldDefinitionDTO field = formSchemaService.createField(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Form field created successfully", field));
    }

    /**
     * Update a form field - Admin only
     */
    @PutMapping("/fields/{fieldId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<FormFieldDefinitionDTO>> updateField(
            @PathVariable Long fieldId,
            @Valid @RequestBody UpdateFormFieldDTO dto) {
        log.info("Updating form field: {}", fieldId);
        FormFieldDefinitionDTO field = formSchemaService.updateField(fieldId, dto);
        return ResponseEntity.ok(ApiResponse.success("Form field updated successfully", field));
    }

    /**
     * Delete a form field - Admin only
     */
    @DeleteMapping("/fields/{fieldId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteField(@PathVariable Long fieldId) {
        log.info("Deleting form field: {}", fieldId);
        formSchemaService.deleteField(fieldId);
        return ResponseEntity.ok(ApiResponse.success("Form field deleted successfully", null));
    }

    /**
     * Bulk create multiple form fields - Admin only
     */
    @PostMapping("/fields/bulk")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<FormFieldDefinitionDTO>>> bulkCreateFields(
            @Valid @RequestBody BulkCreateFieldsDTO dto) {
        log.info("Bulk creating {} fields for case type: {}", dto.getFields().size(), dto.getCaseTypeId());
        List<FormFieldDefinitionDTO> createdFields = formSchemaService.bulkCreateFields(
                dto.getCaseTypeId(), dto.getFields());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fields created successfully", createdFields));
    }

    /**
     * Bulk update multiple form fields - Admin only
     */
    @PutMapping("/fields/bulk")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<List<FormFieldDefinitionDTO>>> bulkUpdateFields(
            @Valid @RequestBody BulkUpdateFieldsDTO dto) {
        log.info("Bulk updating {} fields", dto.getFields().size());
        List<FormFieldDefinitionDTO> updatedFields = formSchemaService.bulkUpdateFields(dto.getFields());
        return ResponseEntity.ok(ApiResponse.success("Fields updated successfully", updatedFields));
    }

    /**
     * Bulk delete multiple form fields - Admin only
     */
    @DeleteMapping("/fields/bulk")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkDeleteFields(
            @Valid @RequestBody BulkDeleteFieldsDTO dto) {
        log.info("Bulk deleting {} fields", dto.getFieldIds().size());
        formSchemaService.bulkDeleteFields(dto.getFieldIds());
        
        Map<String, Object> response = Map.of(
                "message", "Fields deleted successfully",
                "deletedCount", dto.getFieldIds().size()
        );
        return ResponseEntity.ok(ApiResponse.success("Fields deleted successfully", response));
    }


    /**
     * Reorder form fields - Admin only
     */
    @PutMapping("/case-types/{caseTypeId}/fields/reorder")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> reorderFields(
            @PathVariable Long caseTypeId,
            @Valid @RequestBody ReorderFieldsDTO dto) {
        log.info("Reordering fields for case type: {}", caseTypeId);
        formSchemaService.reorderFields(caseTypeId, dto);
        return ResponseEntity.ok(ApiResponse.success("Fields reordered successfully", null));
    }

    /**
     * Validate form data against schema - Public endpoint
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, String>>> validateFormData(
            @Valid @RequestBody ValidateFormDataDTO dto) {
        log.info("Validating form data for case type: {}", dto.getCaseTypeId());
        Map<String, String> errors = formSchemaService.validateFormData(
                dto.getCaseTypeId(), dto.getFormData());

        if (errors.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Form data is valid", errors));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Validation failed"));
        }
    }
}

