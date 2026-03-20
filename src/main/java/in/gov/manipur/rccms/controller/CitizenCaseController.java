package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseDTO;
import in.gov.manipur.rccms.dto.CaseDetailDTO;
import in.gov.manipur.rccms.dto.CaseDocumentDTO;
import in.gov.manipur.rccms.dto.CitizenActionsRequiredDTO;
import in.gov.manipur.rccms.dto.CreateCaseDTO;
import in.gov.manipur.rccms.dto.ResubmitCaseDTO;
import in.gov.manipur.rccms.entity.Case;
import in.gov.manipur.rccms.repository.CaseRepository;
import in.gov.manipur.rccms.repository.CaseWorkflowInstanceRepository;
import in.gov.manipur.rccms.service.ActionsRequiredService;
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

import java.util.List;

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
    private final ActionsRequiredService actionsRequiredService;

    /**
     * Citizen dashboard – "Actions required"
     * GET /api/citizen/dashboard/actions-required
     * Returns count of cases needing citizen action and optional short list (case id, case number, subject, action needed).
     */
    @Operation(
            summary = "Actions required (Citizen)",
            description = "Count and optional list of cases needing citizen action (e.g. acknowledge notice, resubmit after correction). User from auth."
    )
    @GetMapping("/dashboard/actions-required")
    public ResponseEntity<ApiResponse<CitizenActionsRequiredDTO>> getActionsRequired(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        Long applicantId = getApplicantId(request);
        if (applicantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User ID not found"));
        }
        CitizenActionsRequiredDTO dto = actionsRequiredService.getCitizenActionsRequired(applicantId, limit);
        return ResponseEntity.ok(ApiResponse.success("Actions required", dto));
    }

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
     * Get all cases for citizen with document summaries
     * GET /api/citizen/cases
     * Returns all cases belonging to the citizen with available documents (notice, ordersheet, judgement)
     */
    @Operation(
            summary = "Get All My Cases (Citizen)",
            description = "Get all cases belonging to the logged-in citizen with document summaries (notice, ordersheet, judgement) if available."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<CaseDTO>>> getMyCases(HttpServletRequest request) {
        Long applicantId = getApplicantId(request);
        if (applicantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User ID not found"));
        }
        
        List<CaseDTO> cases = caseService.getCasesByApplicant(applicantId);
        log.info("Retrieved {} cases for citizen {}", cases.size(), applicantId);
        
        return ResponseEntity.ok(ApiResponse.success("Cases retrieved successfully", cases));
    }

    /**
     * Get full case detail for citizen (includes case info, workflow history, and all available documents)
     * GET /api/citizen/cases/{caseId}/detail
     */
    @Operation(
            summary = "Get Case Detail (Citizen)",
            description = "Get full case detail including case info, workflow history, and all available documents (notice, ordersheet, judgement). Only accessible by the case applicant."
    )
    @GetMapping("/{caseId}/detail")
    public ResponseEntity<ApiResponse<CaseDetailDTO>> getCaseDetail(
            @PathVariable Long caseId,
            HttpServletRequest request) {
        log.info("Citizen get case detail request: caseId={}", caseId);
        
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
        
        CaseDetailDTO detail = actionsRequiredService.getCaseDetail(caseId);
        return ResponseEntity.ok(ApiResponse.success("Case detail retrieved", detail));
    }

    /**
     * Get notice document for applicant (only if notice is sent to party)
     * GET /api/citizen/cases/{caseId}/documents/NOTICE
     */
    @Operation(
            summary = "Get Notice Document (Citizen)",
            description = "Get latest notice document for a case. Only visible if notice has been sent to party."
    )
    @GetMapping("/{caseId}/documents/NOTICE")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> getNoticeDocument(
            @PathVariable Long caseId,
            HttpServletRequest request) {
        return getDocumentForCitizen(caseId, "NOTICE", request);
    }

    /**
     * Get all notice documents for applicant
     * GET /api/citizen/cases/{caseId}/documents/NOTICE/all
     */
    @Operation(
            summary = "Get All Notice Documents (Citizen)",
            description = "Get all notice documents for a case. Only visible if notices have been finalized."
    )
    @GetMapping("/{caseId}/documents/NOTICE/all")
    public ResponseEntity<ApiResponse<List<CaseDocumentDTO>>> getAllNoticeDocuments(
            @PathVariable Long caseId,
            HttpServletRequest request) {
        return getAllDocumentsForCitizen(caseId, "NOTICE", request);
    }

    /**
     * Get notice draft document for applicant
     * GET /api/citizen/cases/{caseId}/documents/NOTICE_DRAFT
     */
    @Operation(
            summary = "Get Notice Draft Document (Citizen)",
            description = "Get latest notice draft document for a case."
    )
    @GetMapping("/{caseId}/documents/NOTICE_DRAFT")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> getNoticeDraftDocument(
            @PathVariable Long caseId,
            HttpServletRequest request) {
        return getDocumentForCitizen(caseId, "NOTICE", request);
    }

    /**
     * Get all notice draft documents for applicant
     * GET /api/citizen/cases/{caseId}/documents/NOTICE_DRAFT/all
     */
    @Operation(
            summary = "Get All Notice Draft Documents (Citizen)",
            description = "Get all notice draft documents for a case."
    )
    @GetMapping("/{caseId}/documents/NOTICE_DRAFT/all")
    public ResponseEntity<ApiResponse<List<CaseDocumentDTO>>> getAllNoticeDraftDocuments(
            @PathVariable Long caseId,
            HttpServletRequest request) {
        return getAllDocumentsForCitizen(caseId, "NOTICE", request);
    }

    /**
     * Get ordersheet document for applicant
     * GET /api/citizen/cases/{caseId}/documents/ORDERSHEET
     */
    @Operation(
            summary = "Get Ordersheet Document (Citizen)",
            description = "Get latest ordersheet document for a case. Only visible if ordersheet has been finalized."
    )
    @GetMapping("/{caseId}/documents/ORDERSHEET")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> getOrdersheetDocument(
            @PathVariable Long caseId,
            HttpServletRequest request) {
        return getDocumentForCitizen(caseId, "ORDERSHEET", request);
    }

    /**
     * Get all ordersheet documents for applicant
     * GET /api/citizen/cases/{caseId}/documents/ORDERSHEET/all
     */
    @Operation(
            summary = "Get All Ordersheet Documents (Citizen)",
            description = "Get all ordersheet documents for a case. Only visible if ordersheets have been finalized."
    )
    @GetMapping("/{caseId}/documents/ORDERSHEET/all")
    public ResponseEntity<ApiResponse<List<CaseDocumentDTO>>> getAllOrdersheetDocuments(
            @PathVariable Long caseId,
            HttpServletRequest request) {
        return getAllDocumentsForCitizen(caseId, "ORDERSHEET", request);
    }

    /**
     * Get judgement document for applicant
     * GET /api/citizen/cases/{caseId}/documents/JUDGEMENT
     */
    @Operation(
            summary = "Get Judgement Document (Citizen)",
            description = "Get latest judgement document for a case. Only visible if judgement has been finalized."
    )
    @GetMapping("/{caseId}/documents/JUDGEMENT")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> getJudgementDocument(
            @PathVariable Long caseId,
            HttpServletRequest request) {
        return getDocumentForCitizen(caseId, "JUDGEMENT", request);
    }

    /**
     * Get all judgement documents for applicant
     * GET /api/citizen/cases/{caseId}/documents/JUDGEMENT/all
     */
    @Operation(
            summary = "Get All Judgement Documents (Citizen)",
            description = "Get all judgement documents for a case. Only visible if judgements have been finalized."
    )
    @GetMapping("/{caseId}/documents/JUDGEMENT/all")
    public ResponseEntity<ApiResponse<List<CaseDocumentDTO>>> getAllJudgementDocuments(
            @PathVariable Long caseId,
            HttpServletRequest request) {
        return getAllDocumentsForCitizen(caseId, "JUDGEMENT", request);
    }

    /**
     * Helper method to get document for citizen with access control
     */
    private ResponseEntity<ApiResponse<CaseDocumentDTO>> getDocumentForCitizen(
            Long caseId, String moduleType, HttpServletRequest request) {
        log.info("Citizen get {} document request: caseId={}", moduleType, caseId);

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

        // Get latest document
        CaseDocumentDTO document = documentService.getLatestDocument(caseId, moduleType);
        if (document == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(moduleType + " document not found for this case"));
        }

        // Only show SIGNED documents
        if (document.getStatus() == null || 
            !document.getStatus().name().equals("SIGNED")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(moduleType + " is not yet finalized"));
        }

        return ResponseEntity.ok(ApiResponse.success(moduleType + " retrieved", document));
    }

    /**
     * Helper method to get all documents for citizen with access control
     */
    private ResponseEntity<ApiResponse<List<CaseDocumentDTO>>> getAllDocumentsForCitizen(
            Long caseId, String moduleType, HttpServletRequest request) {
        log.info("Citizen get all {} documents request: caseId={}", moduleType, caseId);

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

        // Get all documents
        List<CaseDocumentDTO> documents = documentService.getAllDocuments(caseId, moduleType);
        
        // Filter to only show SIGNED documents
        List<CaseDocumentDTO> filteredDocuments = documents.stream()
                .filter(doc -> doc.getStatus() != null && 
                        doc.getStatus().name().equals("SIGNED"))
                .collect(java.util.stream.Collectors.toList());

        if (filteredDocuments.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("No finalized " + moduleType + " documents found for this case"));
        }

        return ResponseEntity.ok(ApiResponse.success("All " + moduleType + " documents retrieved", filteredDocuments));
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
