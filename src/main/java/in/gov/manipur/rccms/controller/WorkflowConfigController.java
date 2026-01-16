package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.entity.WorkflowDefinition;
import in.gov.manipur.rccms.entity.WorkflowState;
import in.gov.manipur.rccms.entity.WorkflowTransition;
import in.gov.manipur.rccms.entity.WorkflowPermission;
import in.gov.manipur.rccms.repository.WorkflowDefinitionRepository;
import in.gov.manipur.rccms.repository.WorkflowStateRepository;
import in.gov.manipur.rccms.repository.WorkflowTransitionRepository;
import in.gov.manipur.rccms.repository.WorkflowPermissionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Workflow Configuration Controller
 * Admin APIs for configuring workflows, states, transitions, and permissions
 * Note: This is for admin use only. Workflows should be configured carefully.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/workflow")
@RequiredArgsConstructor
@Tag(name = "Workflow Configuration", description = "Admin APIs for workflow configuration")
public class WorkflowConfigController {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowPermissionRepository permissionRepository;

    /**
     * Get all workflow definitions
     * GET /api/admin/workflow/definitions
     */
    @Operation(summary = "Get All Workflows", description = "Get all workflow definitions")
    @GetMapping("/definitions")
    public ResponseEntity<ApiResponse<List<WorkflowDefinition>>> getAllWorkflows() {
        List<WorkflowDefinition> workflows = workflowDefinitionRepository.findAllByOrderByWorkflowNameAsc();
        return ResponseEntity.ok(ApiResponse.success("Workflows retrieved successfully", workflows));
    }

    /**
     * Get workflow by code
     * GET /api/admin/workflow/definitions/{workflowCode}
     */
    @Operation(summary = "Get Workflow by Code", description = "Get workflow definition by workflow code")
    @GetMapping("/definitions/{workflowCode}")
    public ResponseEntity<ApiResponse<WorkflowDefinition>> getWorkflowByCode(@PathVariable String workflowCode) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findByWorkflowCode(workflowCode)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowCode));
        return ResponseEntity.ok(ApiResponse.success("Workflow retrieved successfully", workflow));
    }

    /**
     * Get states for a workflow
     * GET /api/admin/workflow/{workflowId}/states
     */
    @Operation(summary = "Get Workflow States", description = "Get all states for a workflow")
    @GetMapping("/{workflowId}/states")
    public ResponseEntity<ApiResponse<List<WorkflowState>>> getWorkflowStates(@PathVariable Long workflowId) {
        List<WorkflowState> states = workflowStateRepository.findStatesByWorkflowOrdered(workflowId);
        return ResponseEntity.ok(ApiResponse.success("States retrieved successfully", states));
    }

    /**
     * Get transitions for a workflow
     * GET /api/admin/workflow/{workflowId}/transitions
     */
    @Operation(summary = "Get Workflow Transitions", description = "Get all transitions for a workflow")
    @GetMapping("/{workflowId}/transitions")
    public ResponseEntity<ApiResponse<List<WorkflowTransition>>> getWorkflowTransitions(@PathVariable Long workflowId) {
        List<WorkflowTransition> transitions = transitionRepository.findByWorkflowIdAndIsActiveTrue(workflowId);
        return ResponseEntity.ok(ApiResponse.success("Transitions retrieved successfully", transitions));
    }

    /**
     * Get permissions for a transition
     * GET /api/admin/workflow/transitions/{transitionId}/permissions
     */
    @Operation(summary = "Get Transition Permissions", description = "Get all permissions for a transition")
    @GetMapping("/transitions/{transitionId}/permissions")
    public ResponseEntity<ApiResponse<List<WorkflowPermission>>> getTransitionPermissions(@PathVariable Long transitionId) {
        List<WorkflowPermission> permissions = permissionRepository.findByTransitionIdAndIsActiveTrue(transitionId);
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", permissions));
    }
}

