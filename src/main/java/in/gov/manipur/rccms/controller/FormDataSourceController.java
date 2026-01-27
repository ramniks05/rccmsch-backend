package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.service.FormDataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Form Data Source Controller
 * Provides public APIs for fetching dropdown data for dynamic forms
 * Used by frontend to populate conditional dropdowns
 */
@Slf4j
@RestController
@RequestMapping("/api/public/form-data-sources")
@RequiredArgsConstructor
@Tag(name = "Form Data Sources", description = "Public APIs for form dropdown data sources")
public class FormDataSourceController {

    private final FormDataSourceService formDataSourceService;

    /**
     * Get admin units by level
     * GET /api/public/form-data-sources/admin-units?level={level}&parentId={parentId}
     */
    @GetMapping("/admin-units")
    @Operation(
            summary = "Get Admin Units",
            description = "Get admin units for dropdown. Supports hierarchical selection (State -> District -> Sub-Division -> Circle)."
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAdminUnits(
            @Parameter(description = "Unit level: CIRCLE, SUB_DIVISION, DISTRICT, STATE", required = true)
            @RequestParam String level,
            @Parameter(description = "Parent unit ID (for hierarchical selection)")
            @RequestParam(required = false) Long parentId) {
        
        log.info("Get admin units request: level={}, parentId={}", level, parentId);
        
        List<Map<String, Object>> units = formDataSourceService.getAdminUnits(level, parentId);
        
        return ResponseEntity.ok(ApiResponse.success("Admin units retrieved successfully", units));
    }

    /**
     * Get courts by level and optional unit
     * GET /api/public/form-data-sources/courts?courtLevel={level}&unitId={unitId}
     */
    @GetMapping("/courts")
    @Operation(
            summary = "Get Courts",
            description = "Get courts for dropdown filtered by court level and optional unit."
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCourts(
            @Parameter(description = "Court level: CIRCLE, SUB_DIVISION, DISTRICT, STATE", required = true)
            @RequestParam String courtLevel,
            @Parameter(description = "Unit ID (optional)")
            @RequestParam(required = false) Long unitId) {
        
        log.info("Get courts request: level={}, unitId={}", courtLevel, unitId);
        
        List<Map<String, Object>> courts = formDataSourceService.getCourts(courtLevel, unitId);
        
        return ResponseEntity.ok(ApiResponse.success("Courts retrieved successfully", courts));
    }

    /**
     * Get all active acts
     * GET /api/public/form-data-sources/acts
     */
    @GetMapping("/acts")
    @Operation(
            summary = "Get Acts",
            description = "Get all active acts for dropdown."
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActs() {
        log.info("Get acts request");
        
        List<Map<String, Object>> acts = formDataSourceService.getActs();
        
        return ResponseEntity.ok(ApiResponse.success("Acts retrieved successfully", acts));
    }

    /**
     * Get all active case natures
     * GET /api/public/form-data-sources/case-natures
     */
    @GetMapping("/case-natures")
    @Operation(
            summary = "Get Case Natures",
            description = "Get all active case natures for dropdown."
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCaseNatures() {
        log.info("Get case natures request");
        
        List<Map<String, Object>> caseNatures = formDataSourceService.getCaseNatures();
        
        return ResponseEntity.ok(ApiResponse.success("Case natures retrieved successfully", caseNatures));
    }

    /**
     * Get case types by case nature
     * GET /api/public/form-data-sources/case-types?caseNatureId={id}
     */
    @GetMapping("/case-types")
    @Operation(
            summary = "Get Case Types",
            description = "Get case types for a specific case nature for dropdown."
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCaseTypes(
            @Parameter(description = "Case nature ID", required = true)
            @RequestParam Long caseNatureId) {
        
        log.info("Get case types request: caseNatureId={}", caseNatureId);
        
        List<Map<String, Object>> caseTypes = formDataSourceService.getCaseTypes(caseNatureId);
        
        return ResponseEntity.ok(ApiResponse.success("Case types retrieved successfully", caseTypes));
    }
}
