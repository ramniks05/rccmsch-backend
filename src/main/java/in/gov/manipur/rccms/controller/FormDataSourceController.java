package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.ExternalApiDataSourceRequestDTO;
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

    /**
     * Get data from an external API (token-authenticated).
     * Backend logs in to the external API (e.g. CHD Revenue UserLogin), caches the token, then fetches the data endpoint.
     * Use when form field dataSource.type = "API" with apiConfigKey and dataEndpoint.
     *
     * POST body: { "dataSource": "{\"type\":\"API\",\"apiConfigKey\":\"chd-revenue\",\"dataEndpoint\":\"/rccmsapi/YourDataPath\"}" }
     * Or GET: ?dataSource={"type":"API","apiConfigKey":"chd-revenue","dataEndpoint":"/rccmsapi/YourDataPath"}
     */
    @PostMapping("/external-api")
    @Operation(
            summary = "Get data from external API",
            description = "Fetches dropdown/data from an external API that requires login (token). Backend authenticates and calls the data endpoint. Body: dataSource (string), optional runtimeParams (object) e.g. { parentId: \"00003\" }."
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getExternalApiData(
            @Parameter(description = "dataSource string + optional runtimeParams object")
            @RequestBody ExternalApiDataSourceRequestDTO body) {
        if (body == null || body.getDataSource() == null || body.getDataSource().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("dataSource is required in request body"));
        }
        log.info("Get external API data request | dataSource: {} | runtimeParams: {}", body.getDataSource(), body.getRuntimeParams());
        List<Map<String, Object>> options = formDataSourceService.getExternalApiData(body.getDataSource(), body.getRuntimeParams());
        log.info("Get external API data response: {} options", options != null ? options.size() : 0);
        return ResponseEntity.ok(ApiResponse.success("External API data retrieved successfully", options));
    }

    @GetMapping("/external-api")
    @Operation(
            summary = "Get data from external API (GET)",
            description = "Same as POST /external-api but dataSource passed as query parameter. Use POST for long dataSource JSON."
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getExternalApiDataGet(
            @Parameter(description = "dataSource JSON string", required = true)
            @RequestParam String dataSource) {
        log.info("Get external API data request (GET)");
        List<Map<String, Object>> options = formDataSourceService.getExternalApiData(dataSource);
        return ResponseEntity.ok(ApiResponse.success("External API data retrieved successfully", options));
    }
}
