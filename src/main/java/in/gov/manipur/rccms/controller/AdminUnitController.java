package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.AdminUnitDTO;
import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.service.AdminUnitService;
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
 * Administrative Unit Controller
 * Handles CRUD operations for administrative units
 */
@Slf4j
@RestController
@RequestMapping("/api/admin-units")
@RequiredArgsConstructor
@Tag(name = "Administrative Units", description = "Administrative Unit master data CRUD operations")
public class AdminUnitController {

    private final AdminUnitService adminUnitService;

    /**
     * Create a new administrative unit
     * POST /api/admin-units
     */
    @Operation(
            summary = "Create Administrative Unit",
            description = "Create a new administrative unit. Code and LGD code must be unique."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Admin unit created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation errors",
                    content = @Content(schema = @Schema(implementation = in.gov.manipur.rccms.dto.ErrorResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Admin unit code or LGD code already exists",
                    content = @Content(schema = @Schema(implementation = in.gov.manipur.rccms.dto.ErrorResponseDTO.class))
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<AdminUnitDTO>> createAdminUnit(
            @Valid @RequestBody AdminUnitDTO request) {
        log.info("Create admin unit request received: {}", request.getUnitCode());
        
        AdminUnitDTO created = adminUnitService.createAdminUnit(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Admin unit created successfully", created));
    }

    /**
     * Get admin unit by ID
     * GET /api/admin-units/{id}
     */
    @Operation(
            summary = "Get Administrative Unit by ID",
            description = "Retrieve an administrative unit by its ID"
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUnitDTO>> getAdminUnitById(@PathVariable Long id) {
        log.info("Get admin unit by ID request: {}", id);
        
        AdminUnitDTO adminUnit = adminUnitService.getAdminUnitById(id);
        
        return ResponseEntity.ok(ApiResponse.success("Admin unit retrieved successfully", adminUnit));
    }

    /**
     * Get admin unit by code
     * GET /api/admin-units/code/{code}
     */
    @Operation(
            summary = "Get Administrative Unit by Code",
            description = "Retrieve an administrative unit by its code"
    )
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<AdminUnitDTO>> getAdminUnitByCode(@PathVariable String code) {
        log.info("Get admin unit by code request: {}", code);
        
        AdminUnitDTO adminUnit = adminUnitService.getAdminUnitByCode(code);
        
        return ResponseEntity.ok(ApiResponse.success("Admin unit retrieved successfully", adminUnit));
    }

    /**
     * Get all administrative units
     * GET /api/admin-units
     */
    @Operation(
            summary = "Get All Administrative Units",
            description = "Retrieve all administrative units (both active and inactive)"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUnitDTO>>> getAllAdminUnits() {
        log.info("Get all admin units request");
        
        List<AdminUnitDTO> adminUnits = adminUnitService.getAllAdminUnits();
        
        return ResponseEntity.ok(ApiResponse.success("Admin units retrieved successfully", adminUnits));
    }

    /**
     * Get all active administrative units
     * GET /api/admin-units/active
     */
    @Operation(
            summary = "Get Active Administrative Units",
            description = "Retrieve only active administrative units"
    )
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<AdminUnitDTO>>> getActiveAdminUnits() {
        log.info("Get active admin units request");
        
        List<AdminUnitDTO> adminUnits = adminUnitService.getActiveAdminUnits();
        
        return ResponseEntity.ok(ApiResponse.success("Active admin units retrieved successfully", adminUnits));
    }

    /**
     * Get administrative units by level
     * GET /api/admin-units/level/{level}
     */
    @Operation(
            summary = "Get Administrative Units by Level",
            description = "Retrieve administrative units by level (STATE, DISTRICT, SUB_DIVISION, CIRCLE)"
    )
    @GetMapping("/level/{level}")
    public ResponseEntity<ApiResponse<List<AdminUnitDTO>>> getAdminUnitsByLevel(
            @PathVariable AdminUnit.UnitLevel level) {
        log.info("Get admin units by level request: {}", level);
        
        List<AdminUnitDTO> adminUnits = adminUnitService.getAdminUnitsByLevel(level);
        
        return ResponseEntity.ok(ApiResponse.success("Admin units retrieved successfully", adminUnits));
    }

    /**
     * Get administrative units by parent
     * GET /api/admin-units/parent/{parentId}
     */
    @Operation(
            summary = "Get Administrative Units by Parent",
            description = "Retrieve child administrative units by parent unit ID"
    )
    @GetMapping("/parent/{parentId}")
    public ResponseEntity<ApiResponse<List<AdminUnitDTO>>> getAdminUnitsByParent(
            @PathVariable Long parentId) {
        log.info("Get admin units by parent request: {}", parentId);
        
        List<AdminUnitDTO> adminUnits = adminUnitService.getAdminUnitsByParent(parentId);
        
        return ResponseEntity.ok(ApiResponse.success("Admin units retrieved successfully", adminUnits));
    }

    /**
     * Get root units (State level)
     * GET /api/admin-units/root
     */
    @Operation(
            summary = "Get Root Administrative Units",
            description = "Retrieve root administrative units (State level - no parent)"
    )
    @GetMapping("/root")
    public ResponseEntity<ApiResponse<List<AdminUnitDTO>>> getRootUnits() {
        log.info("Get root admin units request");
        
        List<AdminUnitDTO> adminUnits = adminUnitService.getRootUnits();
        
        return ResponseEntity.ok(ApiResponse.success("Root admin units retrieved successfully", adminUnits));
    }

    /**
     * Update administrative unit
     * PUT /api/admin-units/{id}
     */
    @Operation(
            summary = "Update Administrative Unit",
            description = "Update an existing administrative unit by ID"
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUnitDTO>> updateAdminUnit(
            @PathVariable Long id,
            @Valid @RequestBody AdminUnitDTO request) {
        log.info("Update admin unit request for ID: {}", id);
        
        AdminUnitDTO updated = adminUnitService.updateAdminUnit(id, request);
        
        return ResponseEntity.ok(ApiResponse.success("Admin unit updated successfully", updated));
    }

    /**
     * Delete administrative unit (soft delete)
     * DELETE /api/admin-units/{id}
     */
    @Operation(
            summary = "Delete Administrative Unit",
            description = "Soft delete an administrative unit by setting isActive to false"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteAdminUnit(@PathVariable Long id) {
        log.info("Delete admin unit request for ID: {}", id);
        
        adminUnitService.deleteAdminUnit(id);
        
        Map<String, Object> response = Map.of(
                "message", "Admin unit deleted successfully",
                "id", id
        );
        
        return ResponseEntity.ok(ApiResponse.success("Admin unit deleted successfully", response));
    }
}

