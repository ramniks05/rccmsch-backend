package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseDTO;
import in.gov.manipur.rccms.dto.CreateCaseDTO;
import in.gov.manipur.rccms.dto.ResubmitCaseDTO;
import in.gov.manipur.rccms.service.CaseService;
import in.gov.manipur.rccms.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Citizen Case Controller
 * Handles case creation and management for citizens
 * Uses X-User-Id header for authentication (alternative to JWT token)
 */
@Slf4j
@RestController
@RequestMapping("/api/citizen/cases")
@RequiredArgsConstructor
@Tag(name = "Citizen Cases", description = "APIs for citizen case submission and management")
public class CitizenCaseController {

    private final CaseService caseService;
    private final CurrentUserService currentUserService;

    /**
     * Create a new case (Citizen endpoint)
     * POST /api/citizen/cases
     * 
     * Uses X-User-Id header to identify the citizen applicant
     */
    @Operation(
            summary = "Create Case (Citizen)",
            description = "Submit a new petition. User ID is extracted from X-User-Id header or JWT token."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<CaseDTO>> createCase(
            @Valid @RequestBody CreateCaseDTO dto,
            HttpServletRequest request) {
        log.info("Citizen create case request: {}", dto);
        
        // Get applicant ID from token (if JWT is provided) or from request header
        Long applicantId = currentUserService.getCurrentOfficerId(request);
        if (applicantId == null) {
            // Try to get from X-User-Id header (for citizen login without JWT)
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader == null) {
                // Try lowercase variant (browsers may send lowercase)
                userIdHeader = request.getHeader("x-user-id");
            }
            
            if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
                try {
                    applicantId = Long.parseLong(userIdHeader.trim());
                    log.info("Extracted applicant ID from X-User-Id header: {}", applicantId);
                } catch (NumberFormatException e) {
                    log.error("Invalid user ID format in header: {}", userIdHeader);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("Invalid user ID format in X-User-Id header"));
                }
            } else {
                log.warn("No user ID found in token or X-User-Id header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User ID not found. Please provide X-User-Id header or valid JWT token."));
            }
        } else {
            log.info("Extracted applicant ID from JWT token: {}", applicantId);
        }
        
        CaseDTO createdCase = caseService.createCase(dto, applicantId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case created successfully", createdCase));
    }

    /**
     * Resubmit a case after correction (citizen updates case data)
     * PUT /api/citizen/cases/{caseId}/resubmit
     */
    @Operation(
            summary = "Resubmit Case (Citizen)",
            description = "Resubmit a case after correction by updating case data. User ID is extracted from X-User-Id header or JWT token."
    )
    @PutMapping("/{caseId}/resubmit")
    public ResponseEntity<ApiResponse<CaseDTO>> resubmitCase(
            @PathVariable Long caseId,
            @Valid @RequestBody ResubmitCaseDTO dto,
            HttpServletRequest request) {
        log.info("Citizen resubmit case request: caseId={}", caseId);

        // Get applicant ID from token (if JWT is provided) or from request header
        Long applicantId = currentUserService.getCurrentOfficerId(request);
        if (applicantId == null) {
            // Try to get from X-User-Id header
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader == null) {
                userIdHeader = request.getHeader("x-user-id");
            }
            
            if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
                try {
                    applicantId = Long.parseLong(userIdHeader.trim());
                } catch (NumberFormatException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("Invalid user ID format in X-User-Id header"));
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User ID not found. Please provide X-User-Id header or valid JWT token."));
            }
        }

        CaseDTO updatedCase = caseService.resubmitCase(caseId, applicantId, dto);
        return ResponseEntity.ok(ApiResponse.success("Case resubmitted successfully", updatedCase));
    }
}
