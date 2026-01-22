package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.AvailableCourtsDTO;
import in.gov.manipur.rccms.service.CourtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public Court Controller
 * Provides public APIs for court selection based on case nature
 * Used by frontend when filing petitions
 */
@Slf4j
@RestController
@RequestMapping("/api/public/courts")
@RequiredArgsConstructor
@Tag(name = "Public Courts", description = "Public APIs for court selection")
public class PublicCourtController {

    private final CourtService courtService;

    /**
     * Get available courts for a case nature
     * This is the key endpoint for frontend - returns courts based on case nature and user's unit
     * 
     * GET /api/public/courts/available?caseNatureId={id}&unitId={id}
     */
    @GetMapping("/available")
    @Operation(
            summary = "Get Available Courts",
            description = "Get available courts based on case nature selection and user's administrative unit. " +
                         "Returns courts that match the case nature's court level and types, filtered by unit hierarchy."
    )
    public ResponseEntity<ApiResponse<AvailableCourtsDTO>> getAvailableCourts(
            @Parameter(description = "Case nature ID", required = true)
            @RequestParam Long caseNatureId,
            @Parameter(description = "User's administrative unit ID", required = true)
            @RequestParam Long unitId) {
        
        log.info("Get available courts request: caseNatureId={}, unitId={}", caseNatureId, unitId);
        
        AvailableCourtsDTO result = courtService.getAvailableCourts(caseNatureId, unitId);
        
        return ResponseEntity.ok(ApiResponse.success("Available courts retrieved successfully", result));
    }
}
