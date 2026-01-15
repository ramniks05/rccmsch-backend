package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseDTO;
import in.gov.manipur.rccms.dto.CreateCaseDTO;
import in.gov.manipur.rccms.dto.ExecuteTransitionDTO;
import in.gov.manipur.rccms.dto.WorkflowTransitionDTO;
import in.gov.manipur.rccms.entity.WorkflowHistory;
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

    /**
     * Create a new case
     * POST /api/cases
     */
    @Operation(summary = "Create Case", description = "Create a new case. Workflow instance will be automatically initialized.")
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
     * Get cases assigned to officer
     * GET /api/cases/assigned/{officerId}
     */
    @Operation(summary = "Get Cases Assigned to Officer", description = "Retrieve all cases assigned to an officer")
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
     * Get workflow history for a case
     * GET /api/cases/{caseId}/history
     */
    @Operation(summary = "Get Workflow History", description = "Get complete workflow history/audit trail for a case")
    @GetMapping("/{caseId}/history")
    public ResponseEntity<ApiResponse<List<WorkflowHistory>>> getWorkflowHistory(@PathVariable Long caseId) {
        List<WorkflowHistory> history = workflowEngineService.getWorkflowHistory(caseId);
        return ResponseEntity.ok(ApiResponse.success("Workflow history retrieved successfully", history));
    }
}

