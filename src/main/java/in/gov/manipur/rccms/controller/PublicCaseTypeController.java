package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseTypeDTO;
import in.gov.manipur.rccms.service.CaseTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public Case Type Controller (Previously PublicCaseNatureController)
 * Provides public APIs for case type selection
 * Used by frontend when filing petitions
 */
@Slf4j
@RestController
@RequestMapping("/api/public/case-types")
@RequiredArgsConstructor
@Tag(name = "Public Case Types", description = "Public APIs for case type selection")
public class PublicCaseTypeController {

    private final CaseTypeService caseTypeService;

    /**
     * Get case types by case nature (query parameter)
     * GET /api/public/case-types?caseNatureId={caseNatureId}
     * Alternative: GET /api/public/case-types/case-nature/{caseNatureId}
     */
    @GetMapping
    @Operation(
            summary = "Get Case Types by Case Nature (Query Parameter)",
            description = "Get all active case types for a specific case nature using query parameter. " +
                         "Used when user selects a case nature and needs to see available case types (New File, Appeal, etc.). " +
                         "If caseNatureId is not provided, returns all active case types."
    )
    public ResponseEntity<ApiResponse<List<CaseTypeDTO>>> getCaseTypes(
            @Parameter(description = "Case nature ID (optional)")
            @RequestParam(required = false) Long caseNatureId) {
        
        log.info("Get case types request: caseNatureId={}", caseNatureId);
        
        List<CaseTypeDTO> caseTypes;
        if (caseNatureId != null) {
            caseTypes = caseTypeService.getCaseTypesByCaseNature(caseNatureId);
        } else {
            caseTypes = caseTypeService.getAllCaseTypes();
        }
        
        return ResponseEntity.ok(ApiResponse.success("Case types retrieved successfully", caseTypes));
    }

    /**
     * Get case types by case nature (path parameter)
     * GET /api/public/case-types/case-nature/{caseNatureId}
     */
    @GetMapping("/case-nature/{caseNatureId}")
    @Operation(
            summary = "Get Case Types by Case Nature (Path Parameter)",
            description = "Get all active case types for a specific case nature. " +
                         "Used when user selects a case nature and needs to see available case types (New File, Appeal, etc.)"
    )
    public ResponseEntity<ApiResponse<List<CaseTypeDTO>>> getCaseTypesByCaseNature(
            @Parameter(description = "Case nature ID", required = true)
            @PathVariable Long caseNatureId) {
        
        log.info("Get case types request: caseNatureId={}", caseNatureId);
        
        List<CaseTypeDTO> caseTypes = caseTypeService.getCaseTypesByCaseNature(caseNatureId);
        
        return ResponseEntity.ok(ApiResponse.success("Case types retrieved successfully", caseTypes));
    }
}
