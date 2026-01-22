package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseNatureDTO;
import in.gov.manipur.rccms.service.CaseNatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public Case Nature Controller
 * Provides public APIs for case nature selection
 * Used by frontend when filing petitions
 */
@Slf4j
@RestController
@RequestMapping("/api/public/case-natures")
@RequiredArgsConstructor
@Tag(name = "Public Case Natures", description = "Public APIs for case nature selection")
public class PublicCaseNatureController {

    private final CaseNatureService caseNatureService;

    /**
     * Get case natures by case type
     * GET /api/public/case-natures/case-type/{caseTypeId}
     */
    @GetMapping("/case-type/{caseTypeId}")
    @Operation(
            summary = "Get Case Natures by Case Type",
            description = "Get all active case natures for a specific case type. " +
                         "Used when user selects a case type and needs to see available case natures (New File, Appeal, etc.)"
    )
    public ResponseEntity<ApiResponse<List<CaseNatureDTO>>> getCaseNaturesByCaseType(
            @Parameter(description = "Case type ID", required = true)
            @PathVariable Long caseTypeId) {
        
        log.info("Get case natures request: caseTypeId={}", caseTypeId);
        
        List<CaseNatureDTO> caseNatures = caseNatureService.getCaseNaturesByCaseType(caseTypeId);
        
        return ResponseEntity.ok(ApiResponse.success("Case natures retrieved successfully", caseNatures));
    }
}
