package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseNatureDTO;
import in.gov.manipur.rccms.service.CaseNatureService;
import io.swagger.v3.oas.annotations.Operation;
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
 * Case Nature Controller (Previously CaseTypeController)
 * Handles CRUD operations for case natures (MUTATION_GIFT_SALE, PARTITION, etc.)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/case-natures")
@RequiredArgsConstructor
@Tag(name = "Case Natures", description = "Case Nature master data CRUD operations")
public class CaseNatureController {

    private final CaseNatureService caseNatureService;

    /**
     * Create a new case nature
     * POST /api/case-natures
     */
    @Operation(
            summary = "Create Case Nature",
            description = "Create a new case nature. Code and name must be unique."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<CaseNatureDTO>> createCaseNature(
            @Valid @RequestBody CaseNatureDTO request) {
        log.info("Create case nature request received: {}", request.getCode());
        
        CaseNatureDTO created = caseNatureService.createCaseNature(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case nature created successfully", created));
    }

    /**
     * Get case nature by ID
     * GET /api/case-natures/{id}
     */
    @Operation(summary = "Get Case Nature by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseNatureDTO>> getCaseNatureById(@PathVariable Long id) {
        CaseNatureDTO caseNature = caseNatureService.getCaseNatureById(id);
        return ResponseEntity.ok(ApiResponse.success("Case nature retrieved successfully", caseNature));
    }

    /**
     * Get case nature by code
     * GET /api/case-natures/code/{code}
     */
    @Operation(summary = "Get Case Nature by Code")
    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<CaseNatureDTO>> getCaseNatureByCode(@PathVariable String code) {
        CaseNatureDTO caseNature = caseNatureService.getCaseNatureByCode(code);
        return ResponseEntity.ok(ApiResponse.success("Case nature retrieved successfully", caseNature));
    }

    /**
     * Get all case natures
     * GET /api/case-natures
     */
    @Operation(summary = "Get All Case Natures")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CaseNatureDTO>>> getAllCaseNatures() {
        List<CaseNatureDTO> caseNatures = caseNatureService.getAllCaseNatures();
        return ResponseEntity.ok(ApiResponse.success("Case natures retrieved successfully", caseNatures));
    }

    /**
     * Get all active case natures
     * GET /api/case-natures/active
     */
    @Operation(summary = "Get Active Case Natures")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CaseNatureDTO>>> getActiveCaseNatures() {
        List<CaseNatureDTO> caseNatures = caseNatureService.getActiveCaseNatures();
        return ResponseEntity.ok(ApiResponse.success("Active case natures retrieved successfully", caseNatures));
    }

    /**
     * Update case nature
     * PUT /api/case-natures/{id}
     */
    @Operation(summary = "Update Case Nature")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseNatureDTO>> updateCaseNature(
            @PathVariable Long id,
            @Valid @RequestBody CaseNatureDTO request) {
        CaseNatureDTO updated = caseNatureService.updateCaseNature(id, request);
        return ResponseEntity.ok(ApiResponse.success("Case nature updated successfully", updated));
    }

    /**
     * Delete case nature (soft delete)
     * DELETE /api/case-natures/{id}
     */
    @Operation(summary = "Delete Case Nature")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCaseNature(@PathVariable Long id) {
        caseNatureService.deleteCaseNature(id);
        Map<String, Object> response = Map.of("message", "Case nature deleted successfully", "id", id);
        return ResponseEntity.ok(ApiResponse.success("Case nature deleted successfully", response));
    }
}
