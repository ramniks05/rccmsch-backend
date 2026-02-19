package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.service.ActionsRequiredService;
import in.gov.manipur.rccms.service.CaseService;
import in.gov.manipur.rccms.service.CurrentUserService;
import in.gov.manipur.rccms.service.FormSchemaService;
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
import java.util.Map;

/**
 * Case Management Controller
 * Handles case creation, retrieval, and workflow operations
 */
@Slf4j
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Tag(name = "Case Management", description = "APIs for case management and workflow operations")
public class CaseController {

    private final CaseService caseService;
    private final WorkflowEngineService workflowEngineService;
    private final CurrentUserService currentUserService;
    private final FormSchemaService formSchemaService;
    private final ActionsRequiredService actionsRequiredService;

    /**
     * Get form schema for a case type
     * GET /api/cases/form-schema/{caseTypeId}
     */
    @Operation(summary = "Get Form Schema", description = "Get form field definitions for a case type")
    @GetMapping("/form-schema/{caseTypeId}")
    public ResponseEntity<ApiResponse<FormSchemaDTO>> getFormSchema(@PathVariable Long caseTypeId) {
        log.info("Get form schema request: caseTypeId={}", caseTypeId);
        FormSchemaDTO schema = formSchemaService.getFormSchema(caseTypeId);
        return ResponseEntity.ok(ApiResponse.success("Form schema retrieved successfully", schema));
    }

    /**
     * Create a new case
     * POST /api/cases
     */
    @Operation(summary = "Create Case", description = "Create a new case. Form data will be validated against schema. Workflow instance will be automatically initialized.")
    @PostMapping
    public ResponseEntity<ApiResponse<CaseDTO>> createCase(
            @Valid @RequestBody CreateCaseDTO dto,
            HttpServletRequest request) {
        log.info("Create case request: {}", dto);
        
        // Get applicant ID from token (for citizen) or from request header
        Long applicantId = currentUserService.getCurrentOfficerId(request);
        if (applicantId == null) {
            // Try to get from header (for citizen login)
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null) {
                try {
                    applicantId = Long.parseLong(userIdHeader);
                } catch (NumberFormatException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("Invalid user ID"));
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User ID not found"));
            }
        }
        
        CaseDTO createdCase = caseService.createCase(dto, applicantId);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Case created successfully", createdCase));
    }

    /**
     * Resubmit a case after correction (citizen updates case data)
     * PUT /api/cases/{caseId}/resubmit
     */
    @Operation(summary = "Resubmit Case", description = "Resubmit a case after correction by updating case data")
    @PutMapping("/{caseId}/resubmit")
    public ResponseEntity<ApiResponse<CaseDTO>> resubmitCase(
            @PathVariable Long caseId,
            @Valid @RequestBody ResubmitCaseDTO dto,
            HttpServletRequest request) {
        log.info("Resubmit case request: caseId={}", caseId);

        // Get applicant ID from token (for citizen) or from request header
        Long applicantId = currentUserService.getCurrentOfficerId(request);
        if (applicantId == null) {
            String userIdHeader = request.getHeader("X-User-Id");
            if (userIdHeader != null) {
                try {
                    applicantId = Long.parseLong(userIdHeader);
                } catch (NumberFormatException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("Invalid user ID"));
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("User ID not found"));
            }
        }

        CaseDTO updatedCase = caseService.resubmitCase(caseId, applicantId, dto);
        return ResponseEntity.ok(ApiResponse.success("Case resubmitted successfully", updatedCase));
    }

    /**
     * Get case by ID
     * GET /api/cases/{id}
     */
    @Operation(summary = "Get Case by ID", description = "Retrieve case details by case ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseDTO>> getCaseById(@PathVariable Long id) {
        CaseDTO caseDTO = caseService.getCaseById(id);
        return ResponseEntity.ok(ApiResponse.success("Case retrieved successfully", caseDTO));
    }

    /**
     * Get full case detail on demand: case info, workflow history, and documents summary.
     * Frontend can use this for "case detail" view; individual documents at GET /api/cases/{caseId}/documents/{moduleType}.
     * GET /api/cases/{id}/detail
     */
    @Operation(summary = "Get Case Detail", description = "Full case detail: case info, workflow history, and list of documents (notice, ordersheet, judgement). Use for case detail view; fetch document content via /documents/{moduleType} as needed.")
    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<CaseDetailDTO>> getCaseDetail(@PathVariable Long id) {
        CaseDetailDTO detail = actionsRequiredService.getCaseDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Case detail", detail));
    }

    /**
     * Get case by case number
     * GET /api/cases/number/{caseNumber}
     */
    @Operation(summary = "Get Case by Case Number", description = "Retrieve case details by case number")
    @GetMapping("/number/{caseNumber}")
    public ResponseEntity<ApiResponse<CaseDTO>> getCaseByCaseNumber(@PathVariable String caseNumber) {
        CaseDTO caseDTO = caseService.getCaseByCaseNumber(caseNumber);
        return ResponseEntity.ok(ApiResponse.success("Case retrieved successfully", caseDTO));
    }

    /**
     * Get cases by applicant
     * GET /api/cases/applicant/{applicantId}
     */
    @Operation(summary = "Get Cases by Applicant", description = "Retrieve all cases filed by an applicant")
    @GetMapping("/applicant/{applicantId}")
    public ResponseEntity<ApiResponse<List<CaseDTO>>> getCasesByApplicant(@PathVariable Long applicantId) {
        List<CaseDTO> cases = caseService.getCasesByApplicant(applicantId);
        return ResponseEntity.ok(ApiResponse.success("Cases retrieved successfully", cases));
    }

    /**
     * Get cases by unit
     * GET /api/cases/unit/{unitId}
     */
    @Operation(summary = "Get Cases by Unit", description = "Retrieve all cases in a unit")
    @GetMapping("/unit/{unitId}")
    public ResponseEntity<ApiResponse<List<CaseDTO>>> getCasesByUnit(@PathVariable Long unitId) {
        List<CaseDTO> cases = caseService.getCasesByUnit(unitId);
        return ResponseEntity.ok(ApiResponse.success("Cases retrieved successfully", cases));
    }

    /**
     * Get cases by status
     * GET /api/cases/status/{status}
     */
    @Operation(summary = "Get Cases by Status", description = "Retrieve all cases with a specific status")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<CaseDTO>>> getCasesByStatus(@PathVariable String status) {
        List<CaseDTO> cases = caseService.getCasesByStatus(status);
        return ResponseEntity.ok(ApiResponse.success("Cases retrieved successfully", cases));
    }

    /**
     * Officer dashboard – "Actions required"
     * GET /api/cases/dashboard/actions-required
     * Returns count of assigned cases that need action (have at least one available transition) and optional short list.
     */
    @Operation(summary = "Actions required (Officer)", description = "Count and optional list of assigned cases needing action (with current state and available transitions). User from auth.")
    @GetMapping("/dashboard/actions-required")
    public ResponseEntity<ApiResponse<OfficerActionsRequiredDTO>> getOfficerActionsRequired(
            @RequestParam(required = false) Integer limit,
            HttpServletRequest request) {
        Long officerId = currentUserService.getCurrentOfficerId(request);
        String roleCode = currentUserService.getCurrentRoleCode(request);
        Long unitId = currentUserService.getCurrentUnitId(request);
        if (officerId == null || roleCode == null || unitId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Officer information not found. Please login as an officer."));
        }
        OfficerActionsRequiredDTO dto = actionsRequiredService.getOfficerActionsRequired(officerId, roleCode, unitId, limit);
        return ResponseEntity.ok(ApiResponse.success("Actions required", dto));
    }

    /**
     * Get action types (transition codes + labels) that exist in the officer's caseload.
     * Use to build "filter by action" dropdown for My Cases.
     * GET /api/cases/my-cases/action-types
     */
    @Operation(summary = "My Cases action types", description = "List of transition codes/labels that currently exist in the officer's assigned cases (for filter dropdown)")
    @GetMapping("/my-cases/action-types")
    public ResponseEntity<ApiResponse<List<TransitionSummaryDTO>>> getMyCasesActionTypes(HttpServletRequest request) {
        Long officerId = currentUserService.getCurrentOfficerId(request);
        String roleCode = currentUserService.getCurrentRoleCode(request);
        Long unitId = currentUserService.getCurrentUnitId(request);
        if (officerId == null || roleCode == null || unitId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Officer information not found. Please login as an officer."));
        }
        List<TransitionSummaryDTO> list = actionsRequiredService.getOfficerCaseloadActionTypes(officerId, roleCode, unitId);
        return ResponseEntity.ok(ApiResponse.success("Action types in caseload", list));
    }

    /**
     * Get cases assigned to current logged-in officer.
     * Optional filter: transitionCode – only cases where this transition is currently available.
     * GET /api/cases/my-cases
     * GET /api/cases/my-cases?transitionCode=RECORD_HEARING
     */
    @Operation(summary = "Get My Assigned Cases", description = "Retrieve cases assigned to the current officer. Optionally filter by transitionCode (e.g. RECORD_HEARING) to show only cases where that action is available.")
    @GetMapping("/my-cases")
    public ResponseEntity<ApiResponse<List<CaseDTO>>> getMyAssignedCases(
            @RequestParam(required = false) String transitionCode,
            HttpServletRequest request) {
        Long officerId = currentUserService.getCurrentOfficerId(request);
        if (officerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Officer information not found. Please login as an officer."));
        }
        String roleCode = currentUserService.getCurrentRoleCode(request);
        Long unitId = currentUserService.getCurrentUnitId(request);
        List<CaseDTO> cases;
        if (transitionCode != null && !transitionCode.isBlank()) {
            cases = actionsRequiredService.getOfficerCasesFilteredByTransition(officerId, roleCode, unitId, transitionCode);
        } else {
            cases = caseService.getCasesAssignedToOfficer(officerId);
        }
        return ResponseEntity.ok(ApiResponse.success("Cases retrieved successfully", cases));
    }

    /**
     * Get cases assigned to officer (by ID - for admin use)
     * GET /api/cases/assigned/{officerId}
     */
    @Operation(summary = "Get Cases Assigned to Officer", description = "Retrieve all cases assigned to an officer (by ID)")
    @GetMapping("/assigned/{officerId}")
    public ResponseEntity<ApiResponse<List<CaseDTO>>> getCasesAssignedToOfficer(@PathVariable Long officerId) {
        List<CaseDTO> cases = caseService.getCasesAssignedToOfficer(officerId);
        return ResponseEntity.ok(ApiResponse.success("Cases retrieved successfully", cases));
    }

    /**
     * Get available transitions for a case
     * GET /api/cases/{caseId}/transitions
     */
    @Operation(summary = "Get Available Transitions", description = "Get all available workflow transitions for current user")
    @GetMapping("/{caseId}/transitions")
    public ResponseEntity<ApiResponse<List<WorkflowTransitionDTO>>> getAvailableTransitions(
            @PathVariable Long caseId,
            HttpServletRequest request) {
        Long officerId = currentUserService.getCurrentOfficerId(request);
        String roleCode = currentUserService.getCurrentRoleCode(request);
        Long unitId = currentUserService.getCurrentUnitId(request);
        
        if (officerId == null || roleCode == null || unitId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User information not found"));
        }
        
        List<WorkflowTransitionDTO> transitions = workflowEngineService
                .getAvailableTransitions(caseId, officerId, roleCode, unitId);
        return ResponseEntity.ok(ApiResponse.success("Available transitions retrieved successfully", transitions));
    }

    /**
     * Execute workflow transition
     * POST /api/cases/{caseId}/transitions/execute
     */
    @Operation(summary = "Execute Transition", description = "Execute a workflow transition for a case")
    @PostMapping("/{caseId}/transitions/execute")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeTransition(
            @PathVariable Long caseId,
            @Valid @RequestBody ExecuteTransitionDTO dto,
            HttpServletRequest request) {
        Long officerId = currentUserService.getCurrentOfficerId(request);
        String roleCode = currentUserService.getCurrentRoleCode(request);
        Long unitId = currentUserService.getCurrentUnitId(request);
        
        if (officerId == null || roleCode == null || unitId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User information not found"));
        }
        
        log.info("Execute transition request: caseId={}, transitionCode={}, officerId={}", 
                caseId, dto.getTransitionCode(), officerId);

        workflowEngineService.executeTransition(caseId, dto.getTransitionCode(), officerId, roleCode, unitId, dto.getComments());

        Map<String, Object> response = Map.of(
                "caseId", caseId,
                "transitionCode", dto.getTransitionCode(),
                "message", "Transition executed successfully"
        );

        return ResponseEntity.ok(ApiResponse.success("Transition executed successfully", response));
    }

    /**
     * Get transition checklist status
     * GET /api/cases/{caseId}/transitions/{transitionCode}/checklist
     */
    @Operation(summary = "Get Transition Checklist", description = "Get checklist status showing which conditions are met and which are blocking a transition")
    @GetMapping("/{caseId}/transitions/{transitionCode}/checklist")
    public ResponseEntity<ApiResponse<TransitionChecklistDTO>> getTransitionChecklist(
            @PathVariable Long caseId,
            @PathVariable String transitionCode,
            HttpServletRequest request) {
        Long officerId = currentUserService.getCurrentOfficerId(request);
        String roleCode = currentUserService.getCurrentRoleCode(request);
        Long unitId = currentUserService.getCurrentUnitId(request);
        
        if (officerId == null || roleCode == null || unitId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User information not found"));
        }
        
        TransitionChecklistDTO checklist = workflowEngineService
                .getTransitionChecklist(caseId, transitionCode, officerId, roleCode, unitId);
        return ResponseEntity.ok(ApiResponse.success("Checklist retrieved successfully", checklist));
    }

    /**
     * Get workflow history for a case
     * GET /api/cases/{caseId}/history
     */
    @Operation(summary = "Get Workflow History", description = "Get complete workflow history/audit trail for a case")
    @GetMapping("/{caseId}/history")
    public ResponseEntity<ApiResponse<List<WorkflowHistoryDTO>>> getWorkflowHistory(@PathVariable Long caseId) {
        List<WorkflowHistoryDTO> history = workflowEngineService.getWorkflowHistoryDTOs(caseId);
        return ResponseEntity.ok(ApiResponse.success("Workflow history retrieved successfully", history));
    }
}

