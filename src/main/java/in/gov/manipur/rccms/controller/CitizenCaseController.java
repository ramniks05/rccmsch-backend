package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseDTO;
import in.gov.manipur.rccms.dto.CaseDocumentDTO;
import in.gov.manipur.rccms.dto.CreateCaseDTO;
import in.gov.manipur.rccms.dto.ResubmitCaseDTO;
import in.gov.manipur.rccms.entity.Case;
import in.gov.manipur.rccms.entity.ModuleType;
import in.gov.manipur.rccms.repository.CaseRepository;
import in.gov.manipur.rccms.repository.CaseWorkflowInstanceRepository;
import in.gov.manipur.rccms.service.CaseDocumentService;
import in.gov.manipur.rccms.service.CaseService;
import in.gov.manipur.rccms.service.CurrentUserService;
import in.gov.manipur.rccms.service.WorkflowEngineService;
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
    private final CaseDocumentService documentService;
    private final CaseRepository caseRepository;
    private final CaseWorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowEngineService workflowEngineService;

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

    /**
     * Get notice document for applicant (only if notice is sent to party)
     * GET /api/citizen/cases/{caseId}/documents/NOTICE
     */
    @Operation(
            summary = "Get Notice Document (Citizen)",
            description = "Get notice document for a case. Only visible if notice has been sent to party."
    )
    @GetMapping("/{caseId}/documents/NOTICE")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> getNoticeDocument(
            @PathVariable Long caseId,
            HttpServletRequest request) {
        log.info("Citizen get notice request: caseId={}", caseId);

        // Get applicant ID
        Long applicantId = getApplicantId(request);
        if (applicantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User ID not found"));
        }

        // Verify case belongs to applicant
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
        
        if (!caseEntity.getApplicantId().equals(applicantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have access to this case"));
        }

        // Check if notice is sent to party (workflow state should be NOTICE_SENT_TO_PARTY or later)
        String currentStateCode = workflowInstanceRepository.findByCaseId(caseId)
                .map(instance -> instance.getCurrentState() != null ? 
                        instance.getCurrentState().getStateCode() : null)
                .orElse(caseEntity.getStatus());
        
        // States where notice should be visible to applicant
        boolean isNoticeVisible = currentStateCode != null && (
                currentStateCode.equals("NOTICE_SENT_TO_PARTY") ||
                currentStateCode.startsWith("NOTICE_SENT") ||
                currentStateCode.equals("HEARING_SCHEDULED") ||
                currentStateCode.equals("HEARING_COMPLETED") ||
                currentStateCode.startsWith("SDC_") ||
                currentStateCode.startsWith("SDO_") ||
                currentStateCode.equals("APPROVED") ||
                currentStateCode.equals("COMPLETED")
        );
        
        if (!isNoticeVisible) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Notice has not been sent to you yet. Current state: " + 
                            (currentStateCode != null ? currentStateCode : "Unknown")));
        }

        // Get latest notice document
        CaseDocumentDTO notice = documentService.getLatestDocument(caseId, ModuleType.NOTICE);
        if (notice == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Notice document not found"));
        }

        // Only show FINAL or SIGNED notices
        if (notice.getStatus() == null || 
            (!notice.getStatus().name().equals("FINAL") && 
             !notice.getStatus().name().equals("SIGNED"))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Notice is not yet finalized"));
        }

        return ResponseEntity.ok(ApiResponse.success("Notice retrieved", notice));
    }

    /**
     * Accept/Receive notice (Citizen acknowledges receipt)
     * POST /api/citizen/cases/{caseId}/documents/NOTICE/accept
     */
    @Operation(
            summary = "Accept Notice (Citizen)",
            description = "Applicant accepts/receives the notice. This action is recorded in case history."
    )
    @PostMapping("/{caseId}/documents/NOTICE/accept")
    public ResponseEntity<ApiResponse<String>> acceptNotice(
            @PathVariable Long caseId,
            @RequestParam(value = "comments", required = false) String comments,
            HttpServletRequest request) {
        log.info("Citizen accept notice request: caseId={}", caseId);

        // Get applicant ID
        Long applicantId = getApplicantId(request);
        if (applicantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User ID not found"));
        }

        // Verify case belongs to applicant
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
        
        if (!caseEntity.getApplicantId().equals(applicantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You do not have access to this case"));
        }

        // Record notice acceptance in workflow history
        workflowEngineService.recordApplicantNoticeAcceptance(caseId, applicantId, comments);

        return ResponseEntity.ok(ApiResponse.success("Notice accepted successfully", 
                "Notice acceptance has been recorded in case history"));
    }

    /**
     * Helper method to get applicant ID from request
     */
    private Long getApplicantId(HttpServletRequest request) {
        Long applicantId = currentUserService.getCurrentOfficerId(request);
        if (applicantId == null) {
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader == null) {
                userIdHeader = request.getHeader("x-user-id");
            }
            
            if (userIdHeader != null && !userIdHeader.trim().isEmpty()) {
                try {
                    applicantId = Long.parseLong(userIdHeader.trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return applicantId;
    }
}
