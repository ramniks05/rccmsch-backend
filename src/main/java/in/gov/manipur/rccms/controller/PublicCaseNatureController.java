package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseNatureDTO;
import in.gov.manipur.rccms.service.CaseNatureService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/case-natures")
@RequiredArgsConstructor
@Tag(name = "Public Case Natures", description = "Public APIs for case nature selection")
public class PublicCaseNatureController {

    private final CaseNatureService caseNatureService;

    /**
     * Get all active case natures (Public)
     * GET /api/case-natures/active
     */
    @GetMapping("/active")
    @Operation(
            summary = "Get Active Case Natures",
            description = "Get all active case natures. Used by frontend when user needs to select a case nature (legal matter)."
    )
    public ResponseEntity<ApiResponse<List<CaseNatureDTO>>> getActiveCaseNatures() {
        log.info("Get active case natures request (public)");
        
        List<CaseNatureDTO> caseNatures = caseNatureService.getActiveCaseNatures();
        
        return ResponseEntity.ok(ApiResponse.success("Active case natures retrieved successfully", caseNatures));
    }
}
