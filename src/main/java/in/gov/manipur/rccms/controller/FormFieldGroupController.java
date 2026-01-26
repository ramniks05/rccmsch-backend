package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CreateFormFieldGroupDTO;
import in.gov.manipur.rccms.dto.FormFieldGroupDTO;
import in.gov.manipur.rccms.service.FormFieldGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Form Field Group Controller
 * Admin endpoints for managing form field groups (master groups for case type form schemas)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/form-schemas")
@RequiredArgsConstructor
@Tag(name = "Form Field Groups", description = "Admin APIs for managing form field groups")
public class FormFieldGroupController {

    private final FormFieldGroupService formFieldGroupService;

    /**
     * Get active field groups for a case type (for dropdown)
     * GET /api/admin/form-schemas/case-types/{caseTypeId}/field-groups
     * Public endpoint - no authentication required (used for dropdowns)
     */
    @GetMapping("/case-types/{caseTypeId}/field-groups")
    @Operation(
            summary = "Get Active Field Groups",
            description = "Get active field groups for a case type. Used by frontend when creating/editing form fields. Public endpoint."
    )
    public ResponseEntity<ApiResponse<List<FormFieldGroupDTO>>> getFieldGroups(@PathVariable Long caseTypeId) {
        log.info("Getting active field groups for case type: {}", caseTypeId);
        List<FormFieldGroupDTO> groups = formFieldGroupService.getActiveGroups(caseTypeId);
        return ResponseEntity.ok(ApiResponse.success("Field groups retrieved successfully", groups));
    }

    /**
     * Get all field groups (including inactive) for a case type
     * GET /api/admin/form-schemas/case-types/{caseTypeId}/groups
     */
    @GetMapping("/case-types/{caseTypeId}/groups")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Get All Field Groups",
            description = "Get all field groups (active and inactive) for a case type."
    )
    public ResponseEntity<ApiResponse<List<FormFieldGroupDTO>>> getAllGroups(@PathVariable Long caseTypeId) {
        log.info("Getting all field groups for case type: {}", caseTypeId);
        List<FormFieldGroupDTO> groups = formFieldGroupService.getAllGroups(caseTypeId);
        return ResponseEntity.ok(ApiResponse.success("Field groups retrieved successfully", groups));
    }

    /**
     * Get field group by ID
     * GET /api/admin/form-schemas/field-groups/{id}
     */
    @GetMapping("/field-groups/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Get Field Group by ID",
            description = "Get a form field group by ID."
    )
    public ResponseEntity<ApiResponse<FormFieldGroupDTO>> getGroupById(@PathVariable Long id) {
        log.info("Getting field group: {}", id);
        FormFieldGroupDTO group = formFieldGroupService.getGroupById(id);
        return ResponseEntity.ok(ApiResponse.success("Field group retrieved successfully", group));
    }

    /**
     * Create a new field group
     * POST /api/admin/form-schemas/field-groups
     */
    @PostMapping("/field-groups")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Create Field Group",
            description = "Create a new form field group for a case type."
    )
    public ResponseEntity<ApiResponse<FormFieldGroupDTO>> createGroup(
            @Valid @RequestBody CreateFormFieldGroupDTO dto) {
        log.info("Creating field group: caseTypeId={}, groupCode={}", dto.getCaseTypeId(), dto.getGroupCode());
        FormFieldGroupDTO created = formFieldGroupService.createGroup(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Field group created successfully", created));
    }

    /**
     * Update a field group
     * PUT /api/admin/form-schemas/field-groups/{id}
     */
    @PutMapping("/field-groups/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Update Field Group",
            description = "Update an existing form field group."
    )
    public ResponseEntity<ApiResponse<FormFieldGroupDTO>> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody CreateFormFieldGroupDTO dto) {
        log.info("Updating field group: id={}", id);
        FormFieldGroupDTO updated = formFieldGroupService.updateGroup(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Field group updated successfully", updated));
    }

    /**
     * Delete a field group
     * DELETE /api/admin/form-schemas/field-groups/{id}
     */
    @DeleteMapping("/field-groups/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(
            summary = "Delete Field Group",
            description = "Delete a form field group. Note: Fields using this group will have orphaned group codes."
    )
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long id) {
        log.info("Deleting field group: id={}", id);
        formFieldGroupService.deleteGroup(id);
        return ResponseEntity.ok(ApiResponse.success("Field group deleted successfully", null));
    }
}
