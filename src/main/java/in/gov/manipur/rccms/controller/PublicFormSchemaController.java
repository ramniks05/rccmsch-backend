package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.FormSchemaDTO;
import in.gov.manipur.rccms.service.FormSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public Form Schema Controller
 * Provides public APIs for form schemas (used by citizens/lawyers)
 */
@Slf4j
@RestController
@RequestMapping("/api/public/form-schemas")
@RequiredArgsConstructor
@Tag(name = "Form Schemas (Public)", description = "Public APIs for form schema retrieval")
public class PublicFormSchemaController {

    private final FormSchemaService formSchemaService;

    /**
     * Get form schema for a case type (for citizens/lawyers - only active fields, grouped)
     * Public endpoint - no authentication required
     * 
     * GET /api/public/form-schemas/case-types/{caseTypeId}
     */
    @GetMapping("/case-types/{caseTypeId}")
    @Operation(
            summary = "Get Form Schema for Case Type",
            description = "Get form schema (field groups and fields) for a specific case type. " +
                         "Used by citizens/lawyers when filing petitions. Returns only active fields organized by groups."
    )
    public ResponseEntity<ApiResponse<FormSchemaDTO>> getFormSchema(
            @Parameter(description = "Case type ID", required = true)
            @PathVariable Long caseTypeId) {
        try {
            log.info("Public request: Getting form schema for case type: {}", caseTypeId);
            FormSchemaDTO schema = formSchemaService.getFormSchema(caseTypeId);
            
            // Log response details for debugging
            if (schema != null) {
                log.info("Form schema retrieved - caseTypeId: {}, caseTypeName: {}, totalFields: {}, groupsCount: {}", 
                        schema.getCaseTypeId(), 
                        schema.getCaseTypeName(), 
                        schema.getTotalFields(),
                        schema.getGroups() != null ? schema.getGroups().size() : 0);
            } else {
                log.warn("Form schema is null for case type: {}", caseTypeId);
            }
            
            return ResponseEntity.ok(ApiResponse.success("Form schema retrieved successfully", schema));
        } catch (Exception e) {
            log.error("Error getting form schema for case type: {}", caseTypeId, e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }
}
