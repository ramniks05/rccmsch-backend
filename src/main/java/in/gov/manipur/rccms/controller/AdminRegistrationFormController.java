package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CreateRegistrationFormFieldDTO;
import in.gov.manipur.rccms.dto.CreateRegistrationFieldGroupDTO;
import in.gov.manipur.rccms.dto.RegistrationFieldGroupDTO;
import in.gov.manipur.rccms.dto.RegistrationFormFieldDTO;
import in.gov.manipur.rccms.entity.RegistrationFormField;
import in.gov.manipur.rccms.service.RegistrationFieldGroupService;
import in.gov.manipur.rccms.service.RegistrationFormService;
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
 * Admin Registration Form Controller
 * Admin APIs for managing registration form schemas
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/registration-forms")
@RequiredArgsConstructor
@Tag(name = "Registration Forms (Admin)", description = "Admin APIs for registration form schemas")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminRegistrationFormController {

    private final RegistrationFormService registrationFormService;
    private final RegistrationFieldGroupService registrationFieldGroupService;

    /**
     * Get all fields by registration type (including inactive)
     * GET /api/admin/registration-forms/{type}/fields
     */
    @Operation(summary = "Get Registration Fields", description = "Get all registration form fields by type")
    @GetMapping("/{type}/fields")
    public ResponseEntity<ApiResponse<List<RegistrationFormFieldDTO>>> getAllFields(@PathVariable String type) {
        RegistrationFormField.RegistrationType regType = RegistrationFormField.RegistrationType.valueOf(type.toUpperCase());
        List<RegistrationFormFieldDTO> fields = registrationFormService.getAllFields(regType);
        return ResponseEntity.ok(ApiResponse.success("Registration form fields retrieved successfully", fields));
    }

    /**
     * Get active field groups for dropdown
     * GET /api/admin/registration-forms/{type}/field-groups
     */
    @Operation(summary = "Get Field Groups", description = "Get active field groups for registration form type")
    @GetMapping("/{type}/field-groups")
    public ResponseEntity<ApiResponse<List<RegistrationFieldGroupDTO>>> getFieldGroups(@PathVariable String type) {
        RegistrationFormField.RegistrationType regType = RegistrationFormField.RegistrationType.valueOf(type.toUpperCase());
        List<RegistrationFieldGroupDTO> groups = registrationFieldGroupService.getActiveGroups(regType);
        return ResponseEntity.ok(ApiResponse.success("Registration form field groups retrieved successfully", groups));
    }

    /**
     * Get all field groups (including inactive)
     * GET /api/admin/registration-forms/{type}/groups
     */
    @Operation(summary = "Get All Field Groups", description = "Get all field groups for registration form type")
    @GetMapping("/{type}/groups")
    public ResponseEntity<ApiResponse<List<RegistrationFieldGroupDTO>>> getAllGroups(@PathVariable String type) {
        RegistrationFormField.RegistrationType regType = RegistrationFormField.RegistrationType.valueOf(type.toUpperCase());
        List<RegistrationFieldGroupDTO> groups = registrationFieldGroupService.getAllGroups(regType);
        return ResponseEntity.ok(ApiResponse.success("Registration field groups retrieved successfully", groups));
    }

    /**
     * Get field group by ID
     * GET /api/admin/registration-forms/groups/{id}
     */
    @Operation(summary = "Get Field Group by ID", description = "Get a registration field group by ID")
    @GetMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<RegistrationFieldGroupDTO>> getGroupById(@PathVariable Long id) {
        RegistrationFieldGroupDTO group = registrationFieldGroupService.getGroupById(id);
        return ResponseEntity.ok(ApiResponse.success("Registration field group retrieved successfully", group));
    }

    /**
     * Create field group
     * POST /api/admin/registration-forms/groups
     */
    @Operation(summary = "Create Field Group", description = "Create a registration field group")
    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<RegistrationFieldGroupDTO>> createGroup(
            @Valid @RequestBody CreateRegistrationFieldGroupDTO dto) {
        RegistrationFieldGroupDTO created = registrationFieldGroupService.createGroup(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration field group created successfully", created));
    }

    /**
     * Update field group
     * PUT /api/admin/registration-forms/groups/{id}
     */
    @Operation(summary = "Update Field Group", description = "Update a registration field group")
    @PutMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<RegistrationFieldGroupDTO>> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody CreateRegistrationFieldGroupDTO dto) {
        RegistrationFieldGroupDTO updated = registrationFieldGroupService.updateGroup(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Registration field group updated successfully", updated));
    }

    /**
     * Delete field group
     * DELETE /api/admin/registration-forms/groups/{id}
     */
    @Operation(summary = "Delete Field Group", description = "Delete a registration field group")
    @DeleteMapping("/groups/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long id) {
        registrationFieldGroupService.deleteGroup(id);
        return ResponseEntity.ok(ApiResponse.success("Registration field group deleted successfully", null));
    }

    /**
     * Get field by ID
     * GET /api/admin/registration-forms/fields/{id}
     */
    @Operation(summary = "Get Registration Field by ID", description = "Get a registration form field by ID")
    @GetMapping("/fields/{id}")
    public ResponseEntity<ApiResponse<RegistrationFormFieldDTO>> getFieldById(@PathVariable Long id) {
        RegistrationFormFieldDTO field = registrationFormService.getFieldById(id);
        return ResponseEntity.ok(ApiResponse.success("Registration form field retrieved successfully", field));
    }

    /**
     * Create registration field
     * POST /api/admin/registration-forms/fields
     */
    @Operation(summary = "Create Registration Field", description = "Create a registration form field")
    @PostMapping("/fields")
    public ResponseEntity<ApiResponse<RegistrationFormFieldDTO>> createField(
            @Valid @RequestBody CreateRegistrationFormFieldDTO dto) {
        RegistrationFormFieldDTO created = registrationFormService.createField(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration form field created successfully", created));
    }

    /**
     * Update registration field
     * PUT /api/admin/registration-forms/fields/{id}
     */
    @Operation(summary = "Update Registration Field", description = "Update a registration form field")
    @PutMapping("/fields/{id}")
    public ResponseEntity<ApiResponse<RegistrationFormFieldDTO>> updateField(
            @PathVariable Long id,
            @Valid @RequestBody CreateRegistrationFormFieldDTO dto) {
        RegistrationFormFieldDTO updated = registrationFormService.updateField(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Registration form field updated successfully", updated));
    }

    /**
     * Delete registration field
     * DELETE /api/admin/registration-forms/fields/{id}
     */
    @Operation(summary = "Delete Registration Field", description = "Delete a registration form field")
    @DeleteMapping("/fields/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteField(@PathVariable Long id) {
        registrationFormService.deleteField(id);
        return ResponseEntity.ok(ApiResponse.success("Registration form field deleted successfully", null));
    }
}
