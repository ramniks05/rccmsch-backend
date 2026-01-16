package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.WorkflowTransitionDTO;
import in.gov.manipur.rccms.entity.*;
import in.gov.manipur.rccms.exception.InvalidCredentialsException;
import in.gov.manipur.rccms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Workflow Engine Service
 * Core service for executing workflow transitions dynamically
 * Validates permissions, executes transitions, and maintains audit trail
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowEngineService {

    private final CaseWorkflowInstanceRepository instanceRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowPermissionRepository permissionRepository;
    private final WorkflowHistoryRepository historyRepository;
    private final CaseRepository caseRepository;
    private final AdminUnitRepository adminUnitRepository;
    private final OfficerRepository officerRepository;

    /**
     * Check if user can perform a transition
     */
    @Transactional(readOnly = true)
    public boolean canPerformTransition(Long caseId, String transitionCode, Long officerId, String roleCode, Long unitId) {
        log.debug("Checking permission for transition: caseId={}, transitionCode={}, officerId={}, roleCode={}, unitId={}",
                caseId, transitionCode, officerId, roleCode, unitId);

        // Get workflow instance
        CaseWorkflowInstance instance = instanceRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Workflow instance not found for case: " + caseId));

        // Get current state
        WorkflowState currentState = instance.getCurrentState();
        if (currentState == null) {
            throw new RuntimeException("Current state not found for case: " + caseId);
        }

        // Get transition
        WorkflowTransition transition = transitionRepository
                .findByWorkflowIdAndTransitionCode(instance.getWorkflowId(), transitionCode)
                .orElseThrow(() -> new RuntimeException("Transition not found: " + transitionCode));

        // Validate transition is from current state
        if (!transition.getFromStateId().equals(currentState.getId())) {
            log.warn("Transition {} is not valid from current state {}", transitionCode, currentState.getStateCode());
            return false;
        }

        // Check if transition is active
        if (!transition.getIsActive()) {
            log.warn("Transition {} is not active", transitionCode);
            return false;
        }

        // Get unit level
        Long unitIdValue = unitId;
        if (unitIdValue == null) {
            return false;
        }
        AdminUnit unit = adminUnitRepository.findById(unitIdValue)
                .orElseThrow(() -> new RuntimeException("Unit not found: " + unitIdValue));

        // Check permissions
        List<WorkflowPermission> permissions = permissionRepository
                .findPermissionsForTransitionAndRole(transition.getId(), roleCode, unit.getUnitLevel());

        if (permissions.isEmpty()) {
            // Try without unit level constraint
            permissions = permissionRepository
                    .findPermissionsForTransitionAndRoleAnyLevel(transition.getId(), roleCode);
        }

        if (permissions.isEmpty()) {
            log.warn("No permissions found for transition: {}, role: {}, unitLevel: {}", 
                    transitionCode, roleCode, unit.getUnitLevel());
            return false;
        }

        // Check hierarchy rules
        for (WorkflowPermission permission : permissions) {
            if (permission.getCanInitiate() && checkHierarchyRule(permission, unitId, instance)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Execute workflow transition
     */
    public CaseWorkflowInstance executeTransition(Long caseId, String transitionCode, Long officerId, 
                                                   String roleCode, Long unitId, String comments) {
        log.info("Executing transition: caseId={}, transitionCode={}, officerId={}, roleCode={}, unitId={}",
                caseId, transitionCode, officerId, roleCode, unitId);

        // Validate permission
        if (!canPerformTransition(caseId, transitionCode, officerId, roleCode, unitId)) {
            throw new InvalidCredentialsException("You do not have permission to perform this transition");
        }

        // Get workflow instance
        CaseWorkflowInstance instance = instanceRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Workflow instance not found for case: " + caseId));

        // Get transition
        WorkflowTransition transition = transitionRepository
                .findByWorkflowIdAndTransitionCode(instance.getWorkflowId(), transitionCode)
                .orElseThrow(() -> new RuntimeException("Transition not found: " + transitionCode));

        // Get from and to states
        WorkflowState fromState = instance.getCurrentState();
        WorkflowState toState = transition.getToState();

        if (toState == null) {
            throw new RuntimeException("To state not found for transition: " + transitionCode);
        }

        // Get officer
        Long officerIdValue = officerId;
        if (officerIdValue == null) {
            throw new RuntimeException("Officer ID cannot be null");
        }
        Officer officer = officerRepository.findById(officerIdValue)
                .orElseThrow(() -> new RuntimeException("Officer not found: " + officerIdValue));

        // Get unit
        Long unitIdValue = unitId;
        if (unitIdValue == null) {
            throw new RuntimeException("Unit ID cannot be null");
        }
        AdminUnit unit = adminUnitRepository.findById(unitIdValue)
                .orElseThrow(() -> new RuntimeException("Unit not found: " + unitIdValue));

        // Update instance
        instance.setCurrentState(toState);
        Long toStateId = toState.getId();
        if (toStateId == null) {
            throw new RuntimeException("To state ID cannot be null");
        }
        instance.setCurrentStateId(toStateId);
        
        // Update assignment based on to state (can be configured later)
        // For now, keep current assignment or update based on workflow logic
        
        instanceRepository.save(instance);

        // Update case status
        Long caseIdValue = caseId;
        if (caseIdValue == null) {
            throw new RuntimeException("Case ID cannot be null");
        }
        Case caseEntity = caseRepository.findById(caseIdValue)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseIdValue));
        caseEntity.setStatus(toState.getStateCode());
        caseRepository.save(caseEntity);

        // Create history record
        WorkflowHistory history = new WorkflowHistory();
        history.setInstance(instance);
        history.setCaseId(caseIdValue);
        history.setFromState(fromState);
        Long fromStateId = fromState.getId();
        if (fromStateId != null) {
            history.setFromStateId(fromStateId);
        }
        history.setToState(toState);
        history.setToStateId(toStateId);
        history.setTransition(transition);
        Long transitionId = transition.getId();
        if (transitionId == null) {
            throw new RuntimeException("Transition ID cannot be null");
        }
        history.setTransitionId(transitionId);
        history.setPerformedByOfficer(officer);
        history.setPerformedByOfficerId(officerIdValue);
        history.setPerformedByRole(roleCode);
        history.setPerformedAtUnit(unit);
        history.setPerformedAtUnitId(unitIdValue);
        history.setComments(comments);
        historyRepository.save(history);

        log.info("Transition executed successfully: {} -> {} for case {}", 
                fromState.getStateCode(), toState.getStateCode(), caseId);

        return instance;
    }

    /**
     * Get available transitions for current user
     */
    @Transactional(readOnly = true)
    public List<WorkflowTransitionDTO> getAvailableTransitions(Long caseId, Long officerId, String roleCode, Long unitId) {
        log.debug("Getting available transitions for caseId={}, officerId={}, roleCode={}, unitId={}",
                caseId, officerId, roleCode, unitId);

        // Get workflow instance
        CaseWorkflowInstance instance = instanceRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Workflow instance not found for case: " + caseId));

        // Get current state
        WorkflowState currentState = instance.getCurrentState();
        if (currentState == null) {
            return new ArrayList<>();
        }

        // Get all transitions from current state
        List<WorkflowTransition> transitions = transitionRepository
                .findTransitionsFromState(instance.getWorkflowId(), currentState.getId());

        // Get unit level
        Long unitIdValue = unitId;
        if (unitIdValue == null) {
            return new ArrayList<>();
        }
        AdminUnit unit = adminUnitRepository.findById(unitIdValue)
                .orElseThrow(() -> new RuntimeException("Unit not found: " + unitIdValue));

        // Filter transitions based on permissions
        List<WorkflowTransitionDTO> availableTransitions = new ArrayList<>();

        for (WorkflowTransition transition : transitions) {
            if (!transition.getIsActive()) {
                continue;
            }

            // Check permissions
            List<WorkflowPermission> permissions = permissionRepository
                    .findPermissionsForTransitionAndRole(transition.getId(), roleCode, unit.getUnitLevel());

            if (permissions.isEmpty()) {
                permissions = permissionRepository
                        .findPermissionsForTransitionAndRoleAnyLevel(transition.getId(), roleCode);
            }

            for (WorkflowPermission permission : permissions) {
                if (permission.getCanInitiate() && checkHierarchyRule(permission, unitId, instance)) {
                    WorkflowTransitionDTO dto = new WorkflowTransitionDTO();
                    dto.setId(transition.getId());
                    dto.setTransitionCode(transition.getTransitionCode());
                    dto.setTransitionName(transition.getTransitionName());
                    dto.setFromStateCode(currentState.getStateCode());
                    dto.setToStateCode(transition.getToState().getStateCode());
                    dto.setRequiresComment(transition.getRequiresComment());
                    dto.setDescription(transition.getDescription());
                    availableTransitions.add(dto);
                    break; // Add once per transition
                }
            }
        }

        return availableTransitions;
    }

    /**
     * Get workflow history for a case
     */
    @Transactional(readOnly = true)
    public List<WorkflowHistory> getWorkflowHistory(Long caseId) {
        return historyRepository.findCaseHistory(caseId);
    }

    /**
     * Check hierarchy rule
     */
    private boolean checkHierarchyRule(WorkflowPermission permission, Long unitId, CaseWorkflowInstance instance) {
        String hierarchyRule = permission.getHierarchyRule();
        
        if (hierarchyRule == null || hierarchyRule.isEmpty()) {
            return true; // No rule means allowed
        }

        Long unitIdValue = unitId;
        if (unitIdValue == null) {
            return false;
        }

        switch (hierarchyRule.toUpperCase()) {
            case "SAME_UNIT":
                return instance.getAssignedToUnitId() != null && 
                       instance.getAssignedToUnitId().equals(unitIdValue);
            
            case "PARENT_UNIT":
                Long assignedUnitId = instance.getAssignedToUnitId();
                if (assignedUnitId == null) {
                    return false;
                }
                AdminUnit assignedUnit = adminUnitRepository.findById(assignedUnitId)
                        .orElse(null);
                if (assignedUnit == null) {
                    return false;
                }
                Long parentUnitId = assignedUnit.getParentUnitId();
                return parentUnitId != null && parentUnitId.equals(unitIdValue);
            
            case "ANY_UNIT":
                return true;
            
            case "SUPERVISOR":
                // Check if current unit is supervisor of assigned unit
                // This can be enhanced based on role hierarchy
                return true; // Simplified for now
            
            default:
                log.warn("Unknown hierarchy rule: {}", hierarchyRule);
                return true; // Default to allow
        }
    }
}

