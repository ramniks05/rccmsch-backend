package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseTypeDTO;
import in.gov.manipur.rccms.service.CaseTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Case Type Controller
 * Handles CRUD operations for case types
 */
@Slf4j
@RestController
@RequestMapping("/api/case-types")
@RequiredArgsConstructor
@Tag(name = "Case Types", description = "Case Type master data CRUD operations")
public class CaseTypeController {

    private final CaseTypeService caseTypeService;

    /**
     * Create a new case type
     * POST /api/case-types
     */
    @Operation(
            summary = "Create Case Type",
            description = "Create a new case type. Code and name must be unique."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Case type created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation errors",
                    content = @Content(schema = @Schema(implementation = in.gov.manipur.rccms.dto.ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Case type code or name already exists",
                    content = @Content(schema = @Schema(implementation = in.gov.manipur.rccms.dto.ErrorResponseDTO.class))
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CaseTypeDTO>> createCaseType(
            @Valid @RequestBody CaseTypeDTO request) {
        log.info("Create case type request received: {}", request.getCode());
        
        CaseTypeDTO created = caseTypeService.createCaseType(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case type created successfully", created));
    }

    /**
     * Get case type by ID
     * GET /api/case-types/{id}
     */
    @Operation(
            summary = "Get Case Type by ID",
            description = "Retrieve a case type by its ID"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Case type retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Case type not found",
                    content = @Content(schema = @Schema(implementation = in.gov.manipur.rccms.dto.ErrorResponseDTO.class))
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseTypeDTO>> getCaseTypeById(@PathVariable Long id) {
        log.info("Get case type by ID request: {}", id);
        
        CaseTypeDTO caseType = caseTypeService.getCaseTypeById(id);
        
        return ResponseEntity.ok(ApiResponse.success("Case type retrieved successfully", caseType));
    }

    /**
     * Get case type by code
     * GET /api/case-types/code/{code}
     */
    @Operation(
            summary = "Get Case Type by Code",
            description = "Retrieve a case type by its code"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Case type retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Case type not found",
                    content = @Content(schema = @Schema(implementation = in.gov.manipur.rccms.dto.ErrorResponseDTO.class))
            )
    })
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<CaseTypeDTO>> getCaseTypeByCode(@PathVariable String code) {
        log.info("Get case type by code request: {}", code);
        
        CaseTypeDTO caseType = caseTypeService.getCaseTypeByCode(code);
        
        return ResponseEntity.ok(ApiResponse.success("Case type retrieved successfully", caseType));
    }

    /**
     * Get all case types
     * GET /api/case-types
     */
    @Operation(
            summary = "Get All Case Types",
            description = "Retrieve all case types (both active and inactive)"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Case types retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CaseTypeDTO>>> getAllCaseTypes() {
        log.info("Get all case types request");
        
        List<CaseTypeDTO> caseTypes = caseTypeService.getAllCaseTypes();
        
        return ResponseEntity.ok(ApiResponse.success("Case types retrieved successfully", caseTypes));
    }

    /**
     * Get all active case types
     * GET /api/case-types/active
     */
    @Operation(
            summary = "Get Active Case Types",
            description = "Retrieve only active case types"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Active case types retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CaseTypeDTO>>> getActiveCaseTypes() {
        log.info("Get active case types request");
        
        List<CaseTypeDTO> caseTypes = caseTypeService.getActiveCaseTypes();
        
        return ResponseEntity.ok(ApiResponse.success("Active case types retrieved successfully", caseTypes));
    }

    /**
     * Update case type
     * PUT /api/case-types/{id}
     */
    @Operation(
            summary = "Update Case Type",
            description = "Update an existing case type by ID"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Case type updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation errors",
                    content = @Content(schema = @Schema(implementation = in.gov.manipur.rccms.dto.ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Case type not found",
                    content = @Content(schema = @Schema(implementation = in.gov.manipur.rccms.dto.ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Case type code or name already exists",
                    content = @Content(schema = @Schema(implementation = in.gov.manipur.rccms.dto.ErrorResponseDTO.class))
            )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseTypeDTO>> updateCaseType(
            @PathVariable Long id,
            @Valid @RequestBody CaseTypeDTO request) {
        log.info("Update case type request for ID: {}", id);
        
        CaseTypeDTO updated = caseTypeService.updateCaseType(id, request);
        
        return ResponseEntity.ok(ApiResponse.success("Case type updated successfully", updated));
    }

    /**
     * Delete case type (soft delete)
     * DELETE /api/case-types/{id}
     */
    @Operation(
            summary = "Delete Case Type",
            description = "Soft delete a case type by setting isActive to false"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Case type deleted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Case type not found",
                    content = @Content(schema = @Schema(implementation = in.gov.manipur.rccms.dto.ErrorResponseDTO.class))
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCaseType(@PathVariable Long id) {
        log.info("Delete case type request for ID: {}", id);
        
        caseTypeService.deleteCaseType(id);
        
        Map<String, Object> response = Map.of(
                "message", "Case type deleted successfully",
                "id", id
        );
        
        return ResponseEntity.ok(ApiResponse.success("Case type deleted successfully", response));
    }

    /**
     * Hard delete case type (permanent deletion)
     * DELETE /api/case-types/{id}/hard
     */
    @Operation(
            summary = "Hard Delete Case Type",
            description = "Permanently delete a case type from the database"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Case type permanently deleted",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Case type not found",
                    content = @Content(schema = @Schema(implementation = in.gov.manipur.rccms.dto.ErrorResponseDTO.class))
            )
    })
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> hardDeleteCaseType(@PathVariable Long id) {
        log.info("Hard delete case type request for ID: {}", id);
        
        caseTypeService.hardDeleteCaseType(id);
        
        Map<String, Object> response = Map.of(
                "message", "Case type permanently deleted",
                "id", id
        );
        
        return ResponseEntity.ok(ApiResponse.success("Case type permanently deleted", response));
    }
}

