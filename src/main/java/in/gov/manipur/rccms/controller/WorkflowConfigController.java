package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.WorkflowDefinition;
import in.gov.manipur.rccms.entity.WorkflowState;
import in.gov.manipur.rccms.entity.WorkflowTransition;
import in.gov.manipur.rccms.entity.WorkflowPermission;
import in.gov.manipur.rccms.repository.WorkflowDefinitionRepository;
import in.gov.manipur.rccms.repository.WorkflowStateRepository;
import in.gov.manipur.rccms.repository.WorkflowTransitionRepository;
import in.gov.manipur.rccms.repository.WorkflowPermissionRepository;
import in.gov.manipur.rccms.service.WorkflowManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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
@PreAuthorize("hasAuthority('ADMIN')")
public class WorkflowConfigController {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowPermissionRepository permissionRepository;
    private final WorkflowManagementService workflowManagementService;

    // ==================== Workflow Definition APIs ====================

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
     * Get active workflow definitions
     * GET /api/admin/workflow/definitions/active
     */
    @Operation(summary = "Get Active Workflows", description = "Get all active workflow definitions")
    @GetMapping("/definitions/active")
    public ResponseEntity<ApiResponse<List<WorkflowDefinition>>> getActiveWorkflows() {
        List<WorkflowDefinition> workflows = workflowDefinitionRepository.findByIsActiveTrueOrderByWorkflowNameAsc();
        return ResponseEntity.ok(ApiResponse.success("Active workflows retrieved successfully", workflows));
    }

    /**
     * Get workflow by ID
     * GET /api/admin/workflow/definitions/id/{id}
     */
    @Operation(summary = "Get Workflow by ID", description = "Get workflow definition by ID")
    @GetMapping("/definitions/id/{id}")
    public ResponseEntity<ApiResponse<WorkflowDefinition>> getWorkflowById(@PathVariable Long id) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("Workflow retrieved successfully", workflow));
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
     * Create workflow definition
     * POST /api/admin/workflow/definitions
     */
    @Operation(summary = "Create Workflow", description = "Create a new workflow definition")
    @PostMapping("/definitions")
    public ResponseEntity<ApiResponse<WorkflowDefinition>> createWorkflow(@Valid @RequestBody CreateWorkflowDTO dto) {
        WorkflowDefinition workflow = workflowManagementService.createWorkflow(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Workflow created successfully", workflow));
    }

    /**
     * Update workflow definition
     * PUT /api/admin/workflow/definitions/{id}
     */
    @Operation(summary = "Update Workflow", description = "Update an existing workflow definition")
    @PutMapping("/definitions/{id}")
    public ResponseEntity<ApiResponse<WorkflowDefinition>> updateWorkflow(
            @PathVariable Long id,
            @Valid @RequestBody CreateWorkflowDTO dto) {
        WorkflowDefinition workflow = workflowManagementService.updateWorkflow(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Workflow updated successfully", workflow));
    }

    /**
     * Delete workflow definition (soft delete)
     * DELETE /api/admin/workflow/definitions/{id}
     */
    @Operation(summary = "Delete Workflow", description = "Delete a workflow definition (soft delete)")
    @DeleteMapping("/definitions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWorkflow(@PathVariable Long id) {
        workflowManagementService.deleteWorkflow(id);
        return ResponseEntity.ok(ApiResponse.success("Workflow deleted successfully", null));
    }

    // ==================== Workflow State APIs ====================

    /**
     * Get states for a workflow
     * GET /api/admin/workflow/{workflowId}/states
     */
    @Operation(summary = "Get Workflow States", description = "Get all states for a workflow")
    @GetMapping("/{workflowId}/states")
    public ResponseEntity<ApiResponse<List<WorkflowStateDTO>>> getWorkflowStates(@PathVariable Long workflowId) {
        List<WorkflowState> states = workflowStateRepository.findStatesByWorkflowOrdered(workflowId);
        List<WorkflowStateDTO> stateDTOs = states.stream()
                .map(workflowManagementService::convertToStateDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("States retrieved successfully", stateDTOs));
    }

    /**
     * Get state by ID
     * GET /api/admin/workflow/states/{id}
     */
    @Operation(summary = "Get State by ID", description = "Get workflow state by ID")
    @GetMapping("/states/{id}")
    public ResponseEntity<ApiResponse<WorkflowStateDTO>> getStateById(@PathVariable Long id) {
        WorkflowState state = workflowStateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("State not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("State retrieved successfully", 
                workflowManagementService.convertToStateDTO(state)));
    }

    /**
     * Create workflow state
     * POST /api/admin/workflow/{workflowId}/states
     */
    @Operation(summary = "Create State", description = "Create a new workflow state")
    @PostMapping("/{workflowId}/states")
    public ResponseEntity<ApiResponse<WorkflowStateDTO>> createState(
            @PathVariable Long workflowId,
            @Valid @RequestBody CreateStateDTO dto) {
        WorkflowState state = workflowManagementService.createState(workflowId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("State created successfully", 
                        workflowManagementService.convertToStateDTO(state)));
    }

    /**
     * Update workflow state
     * PUT /api/admin/workflow/states/{id}
     */
    @Operation(summary = "Update State", description = "Update an existing workflow state")
    @PutMapping("/states/{id}")
    public ResponseEntity<ApiResponse<WorkflowStateDTO>> updateState(
            @PathVariable Long id,
            @Valid @RequestBody CreateStateDTO dto) {
        WorkflowState state = workflowManagementService.updateState(id, dto);
        return ResponseEntity.ok(ApiResponse.success("State updated successfully", 
                workflowManagementService.convertToStateDTO(state)));
    }

    /**
     * Delete workflow state
     * DELETE /api/admin/workflow/states/{id}
     */
    @Operation(summary = "Delete State", description = "Delete a workflow state")
    @DeleteMapping("/states/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteState(@PathVariable Long id) {
        workflowManagementService.deleteState(id);
        return ResponseEntity.ok(ApiResponse.success("State deleted successfully", null));
    }

    // ==================== Workflow Transition APIs ====================

    /**
     * Get transitions for a workflow (active only)
     * GET /api/admin/workflow/{workflowId}/transitions
     */
    @Operation(summary = "Get Workflow Transitions", description = "Get all active transitions for a workflow")
    @GetMapping("/{workflowId}/transitions")
    public ResponseEntity<ApiResponse<List<WorkflowTransition>>> getWorkflowTransitions(@PathVariable Long workflowId) {
        List<WorkflowTransition> transitions = transitionRepository.findByWorkflowIdAndIsActiveTrue(workflowId);
        return ResponseEntity.ok(ApiResponse.success("Transitions retrieved successfully", transitions));
    }

    /**
     * Get all transitions for a workflow (including inactive)
     * GET /api/admin/workflow/{workflowId}/transitions/all
     */
    @Operation(summary = "Get All Workflow Transitions", description = "Get all transitions for a workflow (including inactive)")
    @GetMapping("/{workflowId}/transitions/all")
    public ResponseEntity<ApiResponse<List<WorkflowTransition>>> getAllWorkflowTransitions(@PathVariable Long workflowId) {
        List<WorkflowTransition> transitions = transitionRepository.findByWorkflowId(workflowId);
        return ResponseEntity.ok(ApiResponse.success("All transitions retrieved successfully", transitions));
    }

    /**
     * Get transition by ID
     * GET /api/admin/workflow/transitions/{id}
     */
    @Operation(summary = "Get Transition by ID", description = "Get workflow transition by ID")
    @GetMapping("/transitions/{id}")
    public ResponseEntity<ApiResponse<WorkflowTransition>> getTransitionById(@PathVariable Long id) {
        WorkflowTransition transition = transitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transition not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("Transition retrieved successfully", transition));
    }

    /**
     * Create workflow transition
     * POST /api/admin/workflow/{workflowId}/transitions
     */
    @Operation(summary = "Create Transition", description = "Create a new workflow transition")
    @PostMapping("/{workflowId}/transitions")
    public ResponseEntity<ApiResponse<WorkflowTransition>> createTransition(
            @PathVariable Long workflowId,
            @Valid @RequestBody CreateTransitionDTO dto) {
        WorkflowTransition transition = workflowManagementService.createTransition(workflowId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transition created successfully", transition));
    }

    /**
     * Update workflow transition
     * PUT /api/admin/workflow/transitions/{id}
     */
    @Operation(summary = "Update Transition", description = "Update an existing workflow transition")
    @PutMapping("/transitions/{id}")
    public ResponseEntity<ApiResponse<WorkflowTransition>> updateTransition(
            @PathVariable Long id,
            @Valid @RequestBody CreateTransitionDTO dto) {
        WorkflowTransition transition = workflowManagementService.updateTransition(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Transition updated successfully", transition));
    }

    /**
     * Delete workflow transition
     * DELETE /api/admin/workflow/transitions/{id}
     */
    @Operation(summary = "Delete Transition", description = "Delete a workflow transition")
    @DeleteMapping("/transitions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransition(@PathVariable Long id) {
        workflowManagementService.deleteTransition(id);
        return ResponseEntity.ok(ApiResponse.success("Transition deleted successfully", null));
    }

    // ==================== Workflow Permission APIs ====================

    /**
     * Get permissions for a transition
     * GET /api/admin/workflow/transitions/{transitionId}/permissions
     */
    @Operation(summary = "Get Transition Permissions", description = "Get all permissions for a transition")
    @GetMapping("/transitions/{transitionId}/permissions")
    public ResponseEntity<ApiResponse<List<WorkflowPermissionDTO>>> getTransitionPermissions(@PathVariable Long transitionId) {
        List<WorkflowPermission> permissions = permissionRepository.findByTransitionIdAndIsActiveTrue(transitionId);
        List<WorkflowPermissionDTO> permissionDTOs = permissions.stream()
                .map(workflowManagementService::convertToPermissionDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved successfully", permissionDTOs));
    }

    /**
     * Get conditions for a transition
     * GET /api/admin/workflow/transitions/{transitionId}/conditions
     */
    @Operation(summary = "Get Transition Conditions", description = "Get all conditions for a transition (aggregated from all permissions)")
    @GetMapping("/transitions/{transitionId}/conditions")
    public ResponseEntity<ApiResponse<TransitionConditionsDTO>> getTransitionConditions(@PathVariable Long transitionId) {
        TransitionConditionsDTO conditions = workflowManagementService.getTransitionConditions(transitionId);
        return ResponseEntity.ok(ApiResponse.success("Conditions retrieved successfully", conditions));
    }

    /**
     * Get permission by ID
     * GET /api/admin/workflow/permissions/{id}
     */
    @Operation(summary = "Get Permission by ID", description = "Get workflow permission by ID")
    @GetMapping("/permissions/{id}")
    public ResponseEntity<ApiResponse<WorkflowPermissionDTO>> getPermissionById(@PathVariable Long id) {
        WorkflowPermission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("Permission retrieved successfully", 
                workflowManagementService.convertToPermissionDTO(permission)));
    }

    /**
     * Create workflow permission
     * POST /api/admin/workflow/transitions/{transitionId}/permissions
     */
    @Operation(summary = "Create Permission", description = "Create a new workflow permission")
    @PostMapping("/transitions/{transitionId}/permissions")
    public ResponseEntity<ApiResponse<WorkflowPermissionDTO>> createPermission(
            @PathVariable Long transitionId,
            @Valid @RequestBody CreatePermissionDTO dto) {
        WorkflowPermission permission = workflowManagementService.createPermission(transitionId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Permission created successfully", 
                        workflowManagementService.convertToPermissionDTO(permission)));
    }

    /**
     * Update workflow permission
     * PUT /api/admin/workflow/permissions/{id}
     */
    @Operation(summary = "Update Permission", description = "Update an existing workflow permission")
    @PutMapping("/permissions/{id}")
    public ResponseEntity<ApiResponse<WorkflowPermissionDTO>> updatePermission(
            @PathVariable Long id,
            @Valid @RequestBody CreatePermissionDTO dto) {
        WorkflowPermission permission = workflowManagementService.updatePermission(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Permission updated successfully", 
                workflowManagementService.convertToPermissionDTO(permission)));
    }

    /**
     * Delete workflow permission
     * DELETE /api/admin/workflow/permissions/{id}
     */
    @Operation(summary = "Delete Permission", description = "Delete a workflow permission")
    @DeleteMapping("/permissions/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable Long id) {
        workflowManagementService.deletePermission(id);
        return ResponseEntity.ok(ApiResponse.success("Permission deleted successfully", null));
    }
}

