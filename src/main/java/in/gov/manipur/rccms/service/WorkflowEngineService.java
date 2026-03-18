package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.constant.WorkflowDataKey;
import in.gov.manipur.rccms.dto.ConditionStatusDTO;
import in.gov.manipur.rccms.dto.ModuleFormSchemaDTO;
import in.gov.manipur.rccms.dto.TransitionChecklistDTO;
import in.gov.manipur.rccms.dto.WorkflowHistoryDTO;
import in.gov.manipur.rccms.dto.WorkflowTransitionDTO;
import in.gov.manipur.rccms.entity.*;
import in.gov.manipur.rccms.exception.InvalidCredentialsException;
import in.gov.manipur.rccms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final OfficerDaHistoryRepository postingRepository;
    private final RoleMasterRepository roleMasterRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final CaseDocumentTemplateRepository caseDocumentTemplateRepository;
    private final CaseModuleFormSubmissionRepository moduleFormSubmissionRepository;
    private final CaseModuleFormFieldRepository caseModuleFormFieldRepository;
    private final CaseModuleFormService caseModuleFormService;
    private final ObjectMapper objectMapper;

    /**
     * Check if user can perform a transition. Uses role_id (role_master) when present, else role_code.
     */
    @Transactional(readOnly = true)
    public boolean canPerformTransition(Long caseId, String transitionCode, Long officerId, Long roleId, String roleCode, Long unitId) {
        log.info("[TRANSITION_PERM] canPerformTransition: caseId={}, transitionCode={}, officerId={}, roleId={}, roleCode={}, unitId={}",
                caseId, transitionCode, officerId, roleId, roleCode, unitId);

        CaseWorkflowInstance instance = instanceRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Workflow instance not found for case: " + caseId));

        WorkflowState currentState = instance.getCurrentState();
        if (currentState == null) {
            throw new RuntimeException("Current state not found for case: " + caseId);
        }

        // Find transition(s) from current state with the given code (avoid non-unique results)
        List<WorkflowTransition> candidateTransitions = transitionRepository
                .findTransitionsFromState(instance.getWorkflowId(), currentState.getId())
                .stream()
                .filter(t -> transitionCode.equals(t.getTransitionCode()))
                .toList();
        if (candidateTransitions.isEmpty()) {
            log.warn("[TRANSITION_PERM] FAIL: transition {} not found from current state {}", transitionCode, currentState.getStateCode());
            return false;
        }
        if (candidateTransitions.size() > 1) {
            log.warn("[TRANSITION_PERM] Multiple transitions found for workflowId={}, fromStateId={}, transitionCode={}. Using first.",
                    instance.getWorkflowId(), currentState.getId(), transitionCode);
        }
        WorkflowTransition transition = candidateTransitions.get(0);

        if (!transition.getFromStateId().equals(currentState.getId())) {
            log.warn("[TRANSITION_PERM] FAIL: transition {} is not valid from current state {} (fromStateId={}, currentStateId={})",
                    transitionCode, currentState.getStateCode(), transition.getFromStateId(), currentState.getId());
            return false;
        }

        if (!transition.getIsActive()) {
            log.warn("[TRANSITION_PERM] FAIL: transition {} is not active", transitionCode);
            return false;
        }

        Long unitIdValue = unitId;
        if (unitIdValue == null) {
            log.warn("[TRANSITION_PERM] FAIL: unitId is null (required for permission check)");
            return false;
        }
        AdminUnit unit = adminUnitRepository.findById(unitIdValue)
                .orElseThrow(() -> new RuntimeException("Unit not found: " + unitIdValue));

        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        // Check permissions: prefer role_id when set in DB, always fallback to role_code (workflow_permission may have only role_code)
        List<WorkflowPermission> permissions = new ArrayList<>();
        if (roleId != null && roleId != 0L) {
            permissions = permissionRepository.findPermissionsForTransitionAndRoleId(transition.getId(), roleId, unit.getUnitLevel());
            log.info("[TRANSITION_PERM] By roleId: transitionId={}, roleId={}, unitLevel={} -> {} permission(s)",
                    transition.getId(), roleId, unit.getUnitLevel(), permissions.size());
            if (permissions.isEmpty()) {
                permissions = permissionRepository.findPermissionsForTransitionAndRoleIdAnyLevel(transition.getId(), roleId);
                log.info("[TRANSITION_PERM] By roleId (any level): -> {} permission(s)", permissions.size());
            }
        }
        // Fallback to role_code lookup (required when workflow_permission.role_id is not yet populated)
        String roleCodeToUse = roleCode;
        if ((roleCodeToUse == null || roleCodeToUse.isBlank()) && roleId != null && roleId != 0L) {
            roleCodeToUse = roleMasterRepository.findById(roleId).map(r -> r.getRoleCode()).orElse(null);
            log.info("[TRANSITION_PERM] Resolved roleCode from roleId {} -> {}", roleId, roleCodeToUse);
        }
        if (permissions.isEmpty() && roleCodeToUse != null && !roleCodeToUse.isBlank()) {
            permissions = permissionRepository.findPermissionsForTransitionAndRole(transition.getId(), roleCodeToUse, unit.getUnitLevel());
            log.info("[TRANSITION_PERM] By roleCode: transitionId={}, roleCode={}, unitLevel={} -> {} permission(s)",
                    transition.getId(), roleCodeToUse, unit.getUnitLevel(), permissions.size());
            if (permissions.isEmpty()) {
                permissions = permissionRepository.findPermissionsForTransitionAndRoleAnyLevel(transition.getId(), roleCodeToUse);
                log.info("[TRANSITION_PERM] By roleCode (any level): -> {} permission(s)", permissions.size());
            }
        }

        if (permissions.isEmpty()) {
            log.warn("[TRANSITION_PERM] FAIL: No permissions found for transition: {}, roleId: {}, roleCode: {}, unitLevel: {}",
                    transitionCode, roleId, roleCodeToUse, unit.getUnitLevel());
            return false;
        }

        // Check hierarchy rules and conditions
        for (WorkflowPermission permission : permissions) {
            boolean canInitiate = Boolean.TRUE.equals(permission.getCanInitiate());
            boolean hierarchyOk = checkHierarchyRule(permission, unitId, instance);
            boolean conditionsOk = checkConditions(permission, instance, caseEntity);
            log.info("[TRANSITION_PERM] Permission id={}, roleCode={}, canInitiate={}, hierarchyRule={}, hierarchyOk={}, conditionsOk={}, instance.assignedToUnitId={}",
                    permission.getId(), permission.getRoleCode(), canInitiate, permission.getHierarchyRule(), hierarchyOk, conditionsOk, instance.getAssignedToUnitId());
            if (canInitiate && hierarchyOk && conditionsOk) {
                log.info("[TRANSITION_PERM] OK: permission passed for transition {}", transitionCode);
                return true;
            }
        }

        log.warn("[TRANSITION_PERM] FAIL: No permission passed (canInitiate + hierarchy + conditions). transition={}, permissionsCount={}",
                transitionCode, permissions.size());
        return false;
    }

    /**
     * Execute workflow transition
     * @param assignedOfficerId Optional officer ID for manual assignment (used for REQUEST_FIELD_REPORT)
     */
    public CaseWorkflowInstance executeTransition(Long caseId, String transitionCode, Long officerId,
                                                   Long roleId, String roleCode, Long unitId, String comments, Long assignedOfficerId) {
        log.info("Executing transition: caseId={}, transitionCode={}, officerId={}, roleId={}, roleCode={}, unitId={}, assignedOfficerId={}",
                caseId, transitionCode, officerId, roleId, roleCode, unitId, assignedOfficerId);

        if (!canPerformTransition(caseId, transitionCode, officerId, roleId, roleCode, unitId)) {
            throw new InvalidCredentialsException("You do not have permission to perform this transition");
        }

        // Get workflow instance
        CaseWorkflowInstance instance = instanceRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Workflow instance not found for case: " + caseId));

        // Get from state
        WorkflowState fromState = instance.getCurrentState();
        if (fromState == null) {
            throw new RuntimeException("Current state not found for case: " + caseId);
        }

        // Get transition: filter transitions FROM current state by code to avoid non-unique results
        List<WorkflowTransition> candidateTransitions = transitionRepository
                .findTransitionsFromState(instance.getWorkflowId(), fromState.getId())
                .stream()
                .filter(t -> transitionCode.equals(t.getTransitionCode()))
                .toList();
        if (candidateTransitions.isEmpty()) {
            throw new RuntimeException("Transition not found from current state for code: " + transitionCode);
        }
        if (candidateTransitions.size() > 1) {
            log.warn("Multiple transitions found for workflowId={}, fromStateId={}, transitionCode={}. Using first.",
                    instance.getWorkflowId(), fromState.getId(), transitionCode);
        }
        WorkflowTransition transition = candidateTransitions.get(0);

        // Get to state
        WorkflowState toState = transition.getToState();

        if (toState == null) {
            throw new RuntimeException("To state not found for transition: " + transitionCode);
        }

        // Get officer (optional for citizens)
        Long officerIdValue = officerId;
        Officer officer = null;
        if (officerIdValue != null) {
            officer = officerRepository.findById(officerIdValue)
                    .orElseThrow(() -> new RuntimeException("Officer not found: " + officerIdValue));
        } else if (roleCode == null || !"CITIZEN".equals(roleCode)) {
            throw new RuntimeException("Officer ID is required for role: " + roleCode);
        }

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
        
        // Get case entity to ensure it's loaded for assignment logic
        Long caseIdValue = caseId;
        if (caseIdValue == null) {
            throw new RuntimeException("Case ID cannot be null");
        }
        Case caseEntity = caseRepository.findById(caseIdValue)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseIdValue));
        
        // Handle manual assignment for REQUEST_FIELD_REPORT transition
        if ("REQUEST_FIELD_REPORT".equals(transitionCode) && assignedOfficerId != null) {
            // Manual assignment - assign to specified field officer
            assignCaseToFieldOfficer(instance, caseEntity, assignedOfficerId);
        } else {
            // Normal auto-assignment based on workflow state and permissions
            assignCaseBasedOnWorkflowState(instance, toState, caseEntity);
        }
        
        // Save instance with assignment
        instanceRepository.save(instance);

        // Update case status
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
        history.setPerformedByOfficerId(officerIdValue); // Can be null for citizens
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
     * Manually assign case to a specific field officer
     * Used for REQUEST_FIELD_REPORT transition when officer is selected manually
     */
    private void assignCaseToFieldOfficer(CaseWorkflowInstance instance, Case caseEntity, Long assignedOfficerId) {
        log.info("Manually assigning case {} to field officer {}", instance.getCaseId(), assignedOfficerId);
        
        // Validate assigned officer exists
        Officer assignedOfficer = officerRepository.findById(assignedOfficerId)
                .orElseThrow(() -> new RuntimeException("Assigned officer not found: " + assignedOfficerId));
        
        // Get officer's current posting
        List<OfficerDaHistory> postings = postingRepository.findByOfficerIdAndIsCurrentTrue(assignedOfficerId);
        if (postings.isEmpty()) {
            throw new RuntimeException("Officer " + assignedOfficerId + " does not have an active posting");
        }
        
        // Get the first active posting (officer should have only one active posting)
        OfficerDaHistory posting = postings.get(0);
        
        // Verify this is a unit-based posting (field officer)
        if (posting.getCourtId() != null) {
            throw new RuntimeException("Cannot assign court-based officer for field report. Officer must be unit-based (field officer)");
        }
        
        // Verify officer is a field officer role (PATWARI, KANUNGO, etc.)
        Set<String> fieldOfficerRoles = Set.of("PATWARI", "KANUNGO", "FIELD_OFFICER");
        if (!fieldOfficerRoles.contains(posting.getRoleCode())) {
            log.warn("Warning: Officer {} has role {} which may not be a field officer role", 
                    assignedOfficerId, posting.getRoleCode());
        }
        
        // Verify case has unit assigned (required for field officer assignment)
        if (caseEntity.getUnitId() == null) {
            throw new RuntimeException("Case must have a unit assigned to assign field officer");
        }
        
        // Assign case to the specified field officer
        instance.setAssignedToOfficer(assignedOfficer);
        instance.setAssignedToOfficerId(assignedOfficerId);
        instance.setAssignedToRole(posting.getRoleCode());
        if (posting.getRole() != null) {
            instance.setAssignedToRoleRef(posting.getRole());
            instance.setAssignedToRoleId(posting.getRoleId());
        } else {
            instance.setAssignedToRoleId(posting.getRoleId());
        }
        instance.setAssignedToUnitId(posting.getUnitId());
        log.info("Case {} manually assigned to field officer {} (roleId: {}, roleCode: {}, unit: {})",
                instance.getCaseId(), assignedOfficerId, posting.getRoleId(), posting.getRoleCode(), posting.getUnitId());
    }

    /**
     * Get available transitions for current user
     */
    @Transactional(readOnly = true, noRollbackFor = {RuntimeException.class, IllegalArgumentException.class})
    public List<WorkflowTransitionDTO> getAvailableTransitions(Long caseId, Long officerId, Long roleId, String roleCode, Long unitId) {
        log.info("[GET_AVAILABLE_TRANSITIONS] START: caseId={}, officerId={}, roleId={}, roleCode={}, unitId={}",
                caseId, officerId, roleId, roleCode, unitId);

        // Get workflow instance
        CaseWorkflowInstance instance = null;
        try {
            instance = instanceRepository.findByCaseId(caseId)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Error fetching workflow instance for case {}: {}", caseId, e.getMessage(), e);
            return new ArrayList<>();
        }
        
        if (instance == null) {
            log.warn("Workflow instance not found for case: {}. Case may not have been initialized with a workflow.", caseId);
            return new ArrayList<>();
        }

        // Get current state
        WorkflowState currentState = null;
        try {
            currentState = instance.getCurrentState();
        } catch (Exception e) {
            log.error("Error fetching current state for case {}: {}", caseId, e.getMessage(), e);
            return new ArrayList<>();
        }
        
        if (currentState == null) {
            log.warn("No current state found for case: {}. Workflow instance exists but state is null.", caseId);
            return new ArrayList<>();
        }

        // Get all transitions from current state
        List<WorkflowTransition> transitions = transitionRepository
                .findTransitionsFromState(instance.getWorkflowId(), currentState.getId());

        // Get case entity first
        Case caseEntity = caseRepository.findById(caseId).orElse(null);
        if (caseEntity == null) {
            log.warn("Case not found with ID: {}", caseId);
            return new ArrayList<>();
        }

        // Get unit level - get from case if unitId is null
        Long unitIdValue = unitId;
        if (unitIdValue == null) {
            unitIdValue = caseEntity.getUnitId();
            log.debug("UnitId was null, using case unitId: {} for role: {}", unitIdValue, roleCode);
        }
        
        if (unitIdValue == null) {
            log.warn("Unit ID is null for case: {} and role: {}. Case has no unit assigned.", caseId, roleCode);
            return new ArrayList<>();
        }
        
        AdminUnit unit = adminUnitRepository.findById(unitIdValue).orElse(null);
        if (unit == null) {
            log.warn("Unit not found with ID: {} for case: {}", unitIdValue, caseId);
            return new ArrayList<>();
        }

        // Filter transitions based on permissions
        List<WorkflowTransitionDTO> availableTransitions = new ArrayList<>();

        for (WorkflowTransition transition : transitions) {
            if (!transition.getIsActive()) {
                log.debug("Skipping inactive transition: {}", transition.getTransitionCode());
                continue;
            }

            log.debug("Checking transition: {} for role: {}, unitLevel: {}", 
                    transition.getTransitionCode(), roleCode, unit.getUnitLevel());

            List<WorkflowPermission> permissions = new ArrayList<>();
            if (roleId != null && roleId != 0L) {
                permissions = permissionRepository.findPermissionsForTransitionAndRoleId(transition.getId(), roleId, unit.getUnitLevel());
                if (permissions.isEmpty()) {
                    permissions = permissionRepository.findPermissionsForTransitionAndRoleIdAnyLevel(transition.getId(), roleId);
                }
            }
            if (permissions.isEmpty() && roleCode != null) {
                if ("CITIZEN".equals(roleCode)) {
                    permissions = permissionRepository.findPermissionsForTransitionAndRoleAnyLevel(transition.getId(), roleCode);
                } else {
                    permissions = permissionRepository.findPermissionsForTransitionAndRole(transition.getId(), roleCode, unit.getUnitLevel());
                    if (permissions.isEmpty()) {
                        permissions = permissionRepository.findPermissionsForTransitionAndRoleAnyLevel(transition.getId(), roleCode);
                    }
                }
            }
            log.debug("Found {} permission(s) for transition: {}, roleId: {}, roleCode: {}",
                    permissions.size(), transition.getTransitionCode(), roleId, roleCode);

            for (WorkflowPermission permission : permissions) {
                log.debug("Checking permission - canInitiate: {}, hierarchyRule: {}, unitLevel: {}", 
                        permission.getCanInitiate(), permission.getHierarchyRule(), permission.getUnitLevel());
                
                boolean hierarchyPassed = checkHierarchyRule(permission, unitId, instance);
                log.debug("Hierarchy check result: {} (officer unitId: {}, case assignedToUnitId: {})", 
                        hierarchyPassed, unitId, instance.getAssignedToUnitId());
                
                // Show transitions based on permissions and hierarchy only
                // Conditions are checked when executing, not when showing available transitions
                if (permission.getCanInitiate() && hierarchyPassed) {
                    
                    // Get checklist for this transition
                    TransitionChecklistDTO checklist = null;
                    ModuleFormSchemaDTO formSchema = null;
                    
                    try {
                        log.info("[GET_AVAILABLE_TRANSITIONS] Getting checklist for transition: {} (caseId: {})", 
                                transition.getTransitionCode(), caseId);
                        checklist = getTransitionChecklist(caseId, transition.getTransitionCode(),
                                officerId, roleId, roleCode, unitId);
                        log.info("[GET_AVAILABLE_TRANSITIONS] Successfully got checklist for transition: {} (caseId: {})", 
                                transition.getTransitionCode(), caseId);
                        
                        // Extract module type from checklist conditions to determine if form is needed
                        String moduleTypeStr = extractModuleTypeFromChecklist(checklist);
                        if (moduleTypeStr != null) {
                            try {
                                ModuleType moduleType = ModuleType.valueOf(moduleTypeStr);
                                log.debug("[GET_AVAILABLE_TRANSITIONS] Fetching form schema for moduleType: {} (caseId: {})", 
                                        moduleTypeStr, caseId);
                                // Fetch form schema for this module type
                                formSchema = caseModuleFormService.getFormSchema(
                                        caseEntity.getCaseNatureId(), 
                                        caseEntity.getCaseTypeId(), 
                                        moduleType);
                                log.debug("[GET_AVAILABLE_TRANSITIONS] Successfully fetched form schema for moduleType: {} (caseId: {})", 
                                        moduleTypeStr, caseId);
                            } catch (IllegalArgumentException e) {
                                log.warn("[GET_AVAILABLE_TRANSITIONS] Invalid module type extracted from checklist: {} (caseId: {})", 
                                        moduleTypeStr, caseId);
                            } catch (Exception e) {
                                log.warn("[GET_AVAILABLE_TRANSITIONS] Failed to fetch form schema for module type {} (caseId: {}): {}", 
                                        moduleTypeStr, caseId, e.getMessage(), e);
                            }
                        }
                    } catch (Exception e) {
                        log.error("[GET_AVAILABLE_TRANSITIONS] ERROR getting checklist for transition {} (caseId: {}): {}", 
                                transition.getTransitionCode(), caseId, e.getMessage(), e);
                        // Don't rethrow - continue with other transitions
                    }
                    
                    WorkflowTransitionDTO dto = WorkflowTransitionDTO.builder()
                            .id(transition.getId())
                            .transitionCode(transition.getTransitionCode())
                            .transitionName(transition.getTransitionName())
                            .fromStateCode(currentState.getStateCode())
                            .toStateCode(transition.getToState().getStateCode())
                            .requiresComment(transition.getRequiresComment())
                            .description(transition.getDescription())
                            .checklist(checklist)
                            .formSchema(formSchema)
                            .build();
                    availableTransitions.add(dto);
                    break; // Add once per transition
                }
            }
        }

        log.info("[GET_AVAILABLE_TRANSITIONS] END: caseId={}, found {} available transition(s)", 
                caseId, availableTransitions.size());
        return availableTransitions;
    }

    /**
     * Record applicant notice acceptance (creates history entry without state change)
     */
    @Transactional
    public void recordApplicantNoticeAcceptance(Long caseId, Long applicantId, String comments) {
        log.info("Recording notice acceptance: caseId={}, applicantId={}", caseId, applicantId);
        
        // Get workflow instance
        CaseWorkflowInstance instance = instanceRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Workflow instance not found for case: " + caseId));
        
        // Get current state
        WorkflowState currentState = instance.getCurrentState();
        if (currentState == null) {
            throw new RuntimeException("Current state not found for case: " + caseId);
        }
        
        // Create history record for notice acceptance (no state change, just acknowledgment)
        // Note: We need a dummy transition or handle this differently since transitionId is required
        // For now, we'll use metadata to store the action and save with minimal required fields
        WorkflowHistory history = new WorkflowHistory();
        history.setInstance(instance);
        history.setCaseId(caseId);
        history.setFromState(currentState);
        history.setFromStateId(currentState.getId());
        history.setToState(currentState); // Same state (no transition)
        history.setToStateId(currentState.getId());
        
        // Find a dummy transition or create a special one for applicant actions
        // For now, we'll try to find any transition from current state, or use null handling
        // Since transitionId is required, we need to handle this differently
        // Option: Create a special "APPLICANT_ACKNOWLEDGMENT" transition or use metadata only
        
        // Store in metadata as JSON for applicant actions
        String metadataJson = String.format(
            "{\"action\":\"NOTICE_ACCEPTED\",\"applicantId\":%d,\"type\":\"APPLICANT_ACKNOWLEDGMENT\"}", 
            applicantId);
        
        // Since WorkflowHistory requires transitionId (nullable = false), we need a transition
        // Find any transition from current state to use as a placeholder
        // The metadata will indicate this is an applicant acknowledgment, not a real transition
        List<WorkflowTransition> transitions = transitionRepository
                .findTransitionsFromState(instance.getWorkflowId(), currentState.getId());
        
        if (transitions.isEmpty()) {
            // If no transition exists, store acceptance in workflow data instead
            log.warn("Cannot create history entry: No transition found from state {}. Storing in workflow data.", 
                    currentState.getStateCode());
            updateWorkflowDataFlag(instance, "NOTICE_ACCEPTED_BY_APPLICANT", true);
            return;
        }
        
        // Use first available transition as placeholder (metadata indicates it's an acknowledgment)
        WorkflowTransition placeholderTransition = transitions.get(0);
        history.setTransition(placeholderTransition);
        history.setTransitionId(placeholderTransition.getId());
        
        history.setPerformedByOfficer(null); // Applicant is not an officer
        history.setPerformedByOfficerId(applicantId); // Store applicant ID
        history.setPerformedByRole("CITIZEN");
        history.setPerformedAtUnit(null); // Applicant has no unit
        history.setPerformedAtUnitId(null);
        history.setComments(comments != null ? comments : "Notice received and accepted by applicant");
        history.setMetadata(metadataJson);
        
        historyRepository.save(history);
        
        log.info("Notice acceptance recorded in history for case: {}", caseId);
    }

    /**
     * Helper method to update workflow data flag
     */
    private void updateWorkflowDataFlag(CaseWorkflowInstance instance, String key, boolean value) {
        Map<String, Object> data = parseJsonToMap(instance.getWorkflowData());
        data.put(key, value);
        try {
            instance.setWorkflowData(objectMapper.writeValueAsString(data));
            instanceRepository.save(instance);
        } catch (Exception e) {
            log.error("Failed to update workflow data flag {}: {}", key, e.getMessage());
        }
    }

    /**
     * Record "Application submitted" in workflow history when a case is created.
     * No extra submit step: case submission automatically executes the initial-state entry.
     * Uses a transition that leads TO the initial state (e.g. APPLICATION_SUBMITTED self-loop).
     * If none exists, creates the APPLICATION_SUBMITTED self-loop transition for this workflow so history is always recorded.
     */
    @Transactional
    public void recordApplicationSubmitted(CaseWorkflowInstance instance, WorkflowState initialState, Long applicantId) {
        if (instance == null || initialState == null) return;
        List<WorkflowTransition> intoInitial = transitionRepository
                .findByWorkflowIdAndToStateIdAndIsActiveTrue(instance.getWorkflowId(), initialState.getId());

        WorkflowTransition transition;
        if (intoInitial.isEmpty()) {
            // No transition into initial state (e.g. workflow created via admin without seed). Create APPLICATION_SUBMITTED self-loop so history is always recorded.
            Optional<WorkflowTransition> existingSelfLoop = transitionRepository
                    .findByWorkflowIdAndTransitionCode(instance.getWorkflowId(), "APPLICATION_SUBMITTED")
                    .filter(t -> initialState.getId().equals(t.getFromStateId()) && initialState.getId().equals(t.getToStateId()));
            transition = existingSelfLoop.orElseGet(() -> {
                WorkflowTransition t = new WorkflowTransition();
                t.setWorkflow(instance.getWorkflow());
                t.setWorkflowId(instance.getWorkflowId());
                t.setFromState(initialState);
                t.setFromStateId(initialState.getId());
                t.setToState(initialState);
                t.setToStateId(initialState.getId());
                t.setTransitionCode("APPLICATION_SUBMITTED");
                t.setTransitionName("Application Submitted");
                t.setIsActive(true);
                t.setRequiresComment(false);
                t.setDescription("Case submitted by applicant; no extra submit step.");
                t.setCreatedAt(LocalDateTime.now());
                return transitionRepository.save(t);
            });
        } else {
            transition = intoInitial.stream()
                    .filter(t -> "APPLICATION_SUBMITTED".equals(t.getTransitionCode()) || "CASE_SUBMITTED".equals(t.getTransitionCode())
                            || "SUBMIT_APPLICATION".equals(t.getTransitionCode()))
                    .findFirst()
                    .orElse(intoInitial.get(0));
        }

        WorkflowHistory history = new WorkflowHistory();
        history.setInstance(instance);
        history.setCaseId(instance.getCaseId());
        history.setFromState(transition.getFromState());
        history.setFromStateId(transition.getFromStateId());
        history.setToState(initialState);
        history.setToStateId(initialState.getId());
        history.setTransition(transition);
        history.setTransitionId(transition.getId());
        history.setPerformedByOfficer(null);
        history.setPerformedByOfficerId(applicantId);
        history.setPerformedByRole("CITIZEN");
        history.setPerformedAtUnit(null);
        history.setPerformedAtUnitId(null);
        history.setComments("Application submitted");
        historyRepository.save(history);
        log.info("Application submitted recorded for case {} (initial state: {})", instance.getCaseId(), initialState.getStateCode());
    }

    /**
     * Get role names that can perform the next transition(s) from current state.
     * Used to show "Pending with: Dealing Hand, Presiding Officer" on case info.
     */
    @Transactional(readOnly = true)
    public List<String> getNextTransitionPendingWithRoles(Long caseId) {
        if (caseId == null) return List.of();
        CaseWorkflowInstance instance = instanceRepository.findByCaseId(caseId).orElse(null);
        if (instance == null) return List.of();
        WorkflowState currentState = instance.getCurrentState();
        if (currentState == null) return List.of();
        List<WorkflowTransition> transitions = transitionRepository
                .findTransitionsFromState(instance.getWorkflowId(), currentState.getId());
        Set<String> roleNames = new LinkedHashSet<>();
        for (WorkflowTransition transition : transitions) {
            if (!Boolean.TRUE.equals(transition.getIsActive())) continue;
            List<WorkflowPermission> permissions = permissionRepository.findByTransitionIdAndIsActiveTrue(transition.getId());
            for (WorkflowPermission p : permissions) {
                if (!Boolean.TRUE.equals(p.getCanInitiate())) continue;
                String name = null;
                if (p.getRole() != null) {
                    name = p.getRole().getRoleName();
                }
                if (name == null || name.isBlank()) {
                    name = formatRoleCodeForDisplay(p.getRoleCode());
                }
                if (name != null && !name.isBlank()) roleNames.add(name);
            }
        }
        return new ArrayList<>(roleNames);
    }

    private static String formatRoleCodeForDisplay(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) return null;
        return Arrays.stream(roleCode.split("_"))
                .map(word -> word.isEmpty() ? "" : word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /**
     * Get workflow history for a case
     */
    @Transactional(readOnly = true)
    public List<WorkflowHistory> getWorkflowHistory(Long caseId) {
        return historyRepository.findCaseHistory(caseId);
    }

    /**
     * Get workflow history for a case as DTOs
     */
    @Transactional(readOnly = true)
    public List<WorkflowHistoryDTO> getWorkflowHistoryDTOs(Long caseId) {
        List<WorkflowHistory> historyList = historyRepository.findCaseHistory(caseId);
        return historyList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert WorkflowHistory entity to DTO
     */
    private WorkflowHistoryDTO convertToDTO(WorkflowHistory history) {
        WorkflowHistoryDTO.WorkflowHistoryDTOBuilder builder = WorkflowHistoryDTO.builder()
                .id(history.getId())
                .caseId(history.getCaseId())
                .instanceId(history.getInstanceId())
                .transitionId(history.getTransitionId())
                .fromStateId(history.getFromStateId())
                .toStateId(history.getToStateId())
                .performedByOfficerId(history.getPerformedByOfficerId())
                .performedByRole(history.getPerformedByRole())
                .performedAtUnitId(history.getPerformedAtUnitId())
                .comments(history.getComments())
                .metadata(history.getMetadata())
                .performedAt(history.getPerformedAt());

        // Set transition information if available
        if (history.getTransition() != null) {
            builder.transitionCode(history.getTransition().getTransitionCode())
                   .transitionName(history.getTransition().getTransitionName());
        }

        // Set from state information if available
        if (history.getFromState() != null) {
            builder.fromStateCode(history.getFromState().getStateCode())
                   .fromStateName(history.getFromState().getStateName());
        }

        // Set to state information if available
        if (history.getToState() != null) {
            builder.toStateCode(history.getToState().getStateCode())
                   .toStateName(history.getToState().getStateName());
        }

        // Set officer information if available
        if (history.getPerformedByOfficer() != null) {
            builder.performedByOfficerName(history.getPerformedByOfficer().getFullName());
        }

        // Set unit information if available
        if (history.getPerformedAtUnit() != null) {
            builder.performedAtUnitName(history.getPerformedAtUnit().getUnitName());
        }

        return builder.build();
    }

    /**
     * Check hierarchy rule
     */
    private boolean checkHierarchyRule(WorkflowPermission permission, Long unitId, CaseWorkflowInstance instance) {
        String hierarchyRule = permission.getHierarchyRule();
        
        if (hierarchyRule == null || hierarchyRule.isEmpty()) {
            log.debug("No hierarchy rule specified, allowing access");
            return true; // No rule means allowed
        }

        // ANY_UNIT always passes
        if ("ANY_UNIT".equalsIgnoreCase(hierarchyRule)) {
            log.debug("ANY_UNIT rule - allowing access");
            return true;
        }

        Long unitIdValue = unitId;
        if (unitIdValue == null) {
            log.debug("UnitId is null, hierarchy check failed for rule: {}", hierarchyRule);
            return false;
        }

        switch (hierarchyRule.toUpperCase()) {
            case "SAME_UNIT":
                boolean result = instance.getAssignedToUnitId() != null && 
                       instance.getAssignedToUnitId().equals(unitIdValue);
                log.debug("SAME_UNIT check: unitId={}, case assignedToUnitId={}, result={}", 
                        unitIdValue, instance.getAssignedToUnitId(), result);
                return result;
            
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

    /**
     * Check JSON conditions defined in workflow_permission.conditions
     *
     * Supported keys:
     * - caseTypeCodesAllowed: ["NEW_FILE", "APPEAL", "REVISION", ...] (Case Type codes)
     * - casePriorityIn: ["HIGH", "URGENT"]
     * - caseDataFieldsRequired: ["field1", "field2"]
     * - caseDataFieldEquals: {"fieldName": "expectedValue"}
     * - workflowDataFieldsRequired: ["field1", "field2"]
     */
    private boolean checkConditions(WorkflowPermission permission, CaseWorkflowInstance instance, Case caseEntity) {
        String conditionsJson = permission.getConditions();
        if (conditionsJson == null || conditionsJson.trim().isEmpty()) {
            return true;
        }

        try {
            Map<String, Object> conditions = objectMapper.readValue(conditionsJson, new TypeReference<Map<String, Object>>() {});
            log.info("[TRANSITION_PERM] checkConditions: permissionId={}, conditionKeys={}", permission.getId(), conditions.keySet());

            // caseTypeCodesAllowed
            if (conditions.containsKey("caseTypeCodesAllowed")) {
                List<String> allowed = castToStringList(conditions.get("caseTypeCodesAllowed"));
                String caseTypeCode = caseEntity.getCaseType() != null ? caseEntity.getCaseType().getTypeCode() : null;
                if (caseTypeCode == null || !allowed.contains(caseTypeCode)) {
                    log.warn("[TRANSITION_PERM] Condition FAIL: caseTypeCodesAllowed - caseTypeCode={}, allowed={}", caseTypeCode, allowed);
                    return false;
                }
            }

            // casePriorityIn
            if (conditions.containsKey("casePriorityIn")) {
                List<String> allowed = castToStringList(conditions.get("casePriorityIn"));
                String priority = caseEntity.getPriority();
                if (priority == null || !allowed.contains(priority)) {
                    log.warn("[TRANSITION_PERM] Condition FAIL: casePriorityIn - priority={}, allowed={}", priority, allowed);
                    return false;
                }
            }

            Map<String, Object> caseData = parseJsonToMap(caseEntity.getCaseData());
            if (conditions.containsKey("caseDataFieldsRequired")) {
                List<String> requiredFields = castToStringList(conditions.get("caseDataFieldsRequired"));
                if (!hasRequiredFields(caseData, requiredFields)) {
                    log.warn("[TRANSITION_PERM] Condition FAIL: caseDataFieldsRequired - required={}, caseDataKeys={}", requiredFields, caseData != null ? caseData.keySet() : "null");
                    return false;
                }
            }

            if (conditions.containsKey("caseDataFieldEquals")) {
                Map<String, Object> fieldEquals = castToMap(conditions.get("caseDataFieldEquals"));
                if (!matchesFieldEquals(caseData, fieldEquals)) {
                    log.warn("[TRANSITION_PERM] Condition FAIL: caseDataFieldEquals - expected={}, caseData={}", fieldEquals, caseData);
                    return false;
                }
            }

            Map<String, Object> workflowData = parseJsonToMap(instance.getWorkflowData());
            if (conditions.containsKey("workflowDataFieldsRequired")) {
                List<String> requiredFields = castToStringList(conditions.get("workflowDataFieldsRequired"));
                if (!hasRequiredFields(workflowData, requiredFields)) {
                    log.warn("[TRANSITION_PERM] Condition FAIL: workflowDataFieldsRequired - required={}, workflowDataKeys={}", requiredFields, workflowData != null ? workflowData.keySet() : "null");
                    return false;
                }
            }

            // Document conditions based on allowedDocumentIds and stage flags (allowDocumentDraft / allowDocumentSaveAndSign)
            if (conditions.containsKey("allowedDocumentIds")) {
                List<Long> templateIds = toLongList(conditions.get("allowedDocumentIds"));
                if (templateIds != null && !templateIds.isEmpty()) {
                    Boolean requireDraft = toBoolean(conditions.get("allowDocumentDraft"));
                    Boolean requireSigned = toBoolean(conditions.get("allowDocumentSaveAndSign"));

                    List<DocumentStatus> requiredStatuses = new ArrayList<>();
                    if (Boolean.TRUE.equals(requireDraft)) {
                        requiredStatuses.add(DocumentStatus.DRAFT);
                    }
                    if (Boolean.TRUE.equals(requireSigned)) {
                        requiredStatuses.add(DocumentStatus.SIGNED);
                    }

                    if (!requiredStatuses.isEmpty()) {
                        boolean exists = caseDocumentRepository.existsByCaseIdAndTemplateIdInAndStatusIn(
                                caseEntity.getId(), templateIds, requiredStatuses);
                        if (!exists) {
                            log.warn("[TRANSITION_PERM] Condition FAIL: document condition - caseId={}, templateIds={}, requiredStatuses={}",
                                    caseEntity.getId(), templateIds, requiredStatuses);
                            return false;
                        }
                    }
                }
            }

            return true;
        } catch (Exception e) {
            log.error("Invalid workflow permission conditions JSON: {}", e.getMessage());
            throw new RuntimeException("Invalid workflow permission conditions JSON");
        }
    }

    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Invalid JSON data: {}", e.getMessage());
            return Map.of();
        }
    }

    private List<String> castToStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .toList();
        }
        return List.of(value.toString());
    }

    private Map<String, Object> castToMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            e -> String.valueOf(e.getKey()),
                            Map.Entry::getValue
                    ));
        }
        return Map.of();
    }

    private boolean hasRequiredFields(Map<String, Object> data, List<String> requiredFields) {
        for (String field : requiredFields) {
            Object value = data.get(field);
            if (value == null || value.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesFieldEquals(Map<String, Object> data, Map<String, Object> fieldEquals) {
        for (Map.Entry<String, Object> entry : fieldEquals.entrySet()) {
            String field = entry.getKey();
            Object expected = entry.getValue();
            Object actual = data.get(field);
            if (actual == null || expected == null) {
                return false;
            }
            if (!actual.toString().equals(expected.toString())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Automatically assign case to officer based on workflow state and permissions
     * Finds roles that can handle transitions from the state, then finds officer posted to court
     */
    private void assignCaseBasedOnWorkflowState(CaseWorkflowInstance instance, WorkflowState state, Case caseEntity) {
        log.debug("=== AUTO-ASSIGNMENT DEBUG START ===");
        log.debug("Case ID: {}, State: {} (ID: {}), Workflow ID: {}, Court ID: {}, Unit ID: {}", 
                instance.getCaseId(), state.getStateCode(), state.getId(), 
                instance.getWorkflowId(), 
                caseEntity != null ? caseEntity.getCourtId() : "NULL",
                caseEntity != null ? caseEntity.getUnitId() : "NULL");
        
        if (caseEntity == null) {
            log.warn("Case entity is null. Cannot auto-assign to officer.");
            log.debug("=== AUTO-ASSIGNMENT DEBUG END (NO CASE ENTITY) ===");
            return;
        }
        
        // Check if case has either court or unit (required for assignment)
        if (caseEntity.getCourtId() == null && caseEntity.getUnitId() == null) {
            log.warn("Case {} has neither court nor unit assigned. Cannot auto-assign to officer.", instance.getCaseId());
            log.debug("=== AUTO-ASSIGNMENT DEBUG END (NO COURT OR UNIT) ===");
            return;
        }

        // Validate state and workflow IDs
        if (state == null || state.getId() == null) {
            log.warn("State is null or has no ID for case {}. Cannot auto-assign.", instance.getCaseId());
            log.debug("=== AUTO-ASSIGNMENT DEBUG END (NO STATE ID) ===");
            return;
        }
        
        if (instance.getWorkflowId() == null) {
            log.warn("Workflow ID is null for case {}. Cannot auto-assign.", instance.getCaseId());
            log.debug("=== AUTO-ASSIGNMENT DEBUG END (NO WORKFLOW ID) ===");
            return;
        }
        
        // Find roles that have permissions for transitions FROM this state
        log.debug("Step 1: Finding roles for state {} (ID: {}) in workflow {}...", 
                state.getStateCode(), state.getId(), instance.getWorkflowId());
        
        // DEBUG: Verify state details
        log.debug("  [DEBUG] State details - Code: {}, ID: {}, Name: {}", 
                state.getStateCode(), state.getId(), state.getStateName());
        log.debug("  [DEBUG] Workflow instance - WorkflowId: {}, CaseId: {}", 
                instance.getWorkflowId(), instance.getCaseId());
        
        List<Long> roleIdsForState = getRoleIdsForState(instance.getWorkflowId(), state.getId());
        log.debug("Step 2: Found {} role(s) for state {}: {}", roleIdsForState.size(), state.getStateCode(), roleIdsForState);

        if (roleIdsForState.isEmpty()) {
            log.warn("No roles found with permissions for state {} (ID: {}). Case {} will remain unassigned.", 
                    state.getStateCode(), state.getId(), instance.getCaseId());
            log.debug("=== AUTO-ASSIGNMENT DEBUG END (NO ROLES) ===");
            return;
        }

        // Define unit-based field officer roles (not linked to court)
        Set<String> unitBasedRoles = Set.of("PATWARI", "KANUNGO", "FIELD_OFFICER");
        
        // Get case unit for unit-based officer assignment
        Long caseUnitId = caseEntity.getUnitId();
        AdminUnit caseUnit = null;
        if (caseUnitId != null) {
            caseUnit = adminUnitRepository.findById(caseUnitId).orElse(null);
        }
        
        // Debug: Check all officers posted to this court (if court exists)
        List<OfficerDaHistory> allCourtPostings = new ArrayList<>();
        if (caseEntity.getCourtId() != null) {
            allCourtPostings = postingRepository.findByCourtIdAndIsCurrentTrue(caseEntity.getCourtId());
            log.debug("Step 3: Checking officers posted to court {} (ID: {})...", 
                    caseEntity.getCourtId(), caseEntity.getCourtId());
            log.debug("Total active postings at court {}: {}", caseEntity.getCourtId(), allCourtPostings.size());
            for (OfficerDaHistory posting : allCourtPostings) {
                log.debug("  - Officer ID: {}, Role: {}, UserID: {}, Type: COURT_BASED", 
                        posting.getOfficerId(), posting.getRoleCode(), posting.getPostingUserid());
            }
        } else {
            log.debug("Step 3: Case has no court assigned. Skipping court-based posting check.");
        }
        
        // Debug: Check unit-based officers if case has unit
        if (caseUnitId != null) {
            List<OfficerDaHistory> allUnitPostings = postingRepository.findByUnitIdAndIsCurrentTrue(caseUnitId);
            log.debug("Step 3b: Checking unit-based officers in unit {} (ID: {})...", 
                    caseUnit != null ? caseUnit.getUnitName() : "Unknown", caseUnitId);
            log.debug("Total active unit-based postings in unit {}: {}", caseUnitId, allUnitPostings.size());
            for (OfficerDaHistory posting : allUnitPostings) {
                log.debug("  - Officer ID: {}, Role: {}, UserID: {}, Type: UNIT_BASED", 
                        posting.getOfficerId(), posting.getRoleCode(), posting.getPostingUserid());
            }
        }

        log.debug("Step 4: Searching for officers with roleIds: {}...", roleIdsForState);
        for (Long roleId : roleIdsForState) {
            RoleMaster role = roleMasterRepository.findById(roleId).orElse(null);
            if (role == null) continue;
            String roleCode = role.getRoleCode();
            boolean isUnitBasedRole = unitBasedRoles.contains(roleCode);

            if (isUnitBasedRole) {
                if (caseUnitId == null) {
                    log.warn("Case {} has no unit assigned. Cannot assign unit-based role {}.", instance.getCaseId(), roleCode);
                    continue;
                }
                List<OfficerDaHistory> unitPostings = postingRepository.findByUnitIdAndRoleIdAndIsCurrentTrue(caseUnitId, roleId);
                if (!unitPostings.isEmpty()) {
                    OfficerDaHistory p = unitPostings.get(0);
                    instance.setAssignedToOfficer(p.getOfficer());
                    instance.setAssignedToOfficerId(p.getOfficerId());
                    instance.setAssignedToRole(roleCode);
                    instance.setAssignedToRoleId(roleId);
                    instance.setAssignedToRoleRef(role);
                    instance.setAssignedToUnitId(caseUnitId);
                    log.info("Case {} auto-assigned to unit-based officer {} (roleId: {}, roleCode: {}) based on state {}. Unit: {}, Officer ID: {}",
                            instance.getCaseId(), p.getOfficerId(), roleId, roleCode, state.getStateCode(), caseUnitId, p.getOfficerId());
                    log.debug("=== AUTO-ASSIGNMENT DEBUG END (SUCCESS - UNIT_BASED) ===");
                    return;
                }
                if (caseUnit != null && caseUnit.getParentUnitId() != null) {
                    List<OfficerDaHistory> parentUnitPostings = postingRepository.findByUnitIdAndRoleIdAndIsCurrentTrue(caseUnit.getParentUnitId(), roleId);
                    if (!parentUnitPostings.isEmpty()) {
                        OfficerDaHistory p = parentUnitPostings.get(0);
                        instance.setAssignedToOfficer(p.getOfficer());
                        instance.setAssignedToOfficerId(p.getOfficerId());
                        instance.setAssignedToRole(roleCode);
                        instance.setAssignedToRoleId(roleId);
                        instance.setAssignedToRoleRef(role);
                        instance.setAssignedToUnitId(caseUnit.getParentUnitId());
                        log.info("Case {} auto-assigned to unit-based officer {} (roleId: {}, roleCode: {}) in parent unit {} based on state {}. Officer ID: {}",
                                instance.getCaseId(), p.getOfficerId(), roleId, roleCode, caseUnit.getParentUnitId(), state.getStateCode(), p.getOfficerId());
                        log.debug("=== AUTO-ASSIGNMENT DEBUG END (SUCCESS - UNIT_BASED PARENT) ===");
                        return;
                    }
                }
                log.warn("No unit-based officer found for roleId {} (roleCode: {}) in unit {} (case {}).", roleId, roleCode, caseUnitId, instance.getCaseId());
            } else {
                if (caseEntity.getCourtId() == null) {
                    log.warn("Cannot assign court-based role {} to case {} - case has no court assigned.", roleCode, instance.getCaseId());
                    continue;
                }
                Optional<OfficerDaHistory> posting = postingRepository.findByCourtIdAndRoleIdAndIsCurrentTrue(caseEntity.getCourtId(), roleId);
                if (posting.isPresent()) {
                    OfficerDaHistory p = posting.get();
                    instance.setAssignedToOfficer(p.getOfficer());
                    instance.setAssignedToOfficerId(p.getOfficerId());
                    instance.setAssignedToRole(roleCode);
                    instance.setAssignedToRoleId(roleId);
                    instance.setAssignedToRoleRef(role);
                    log.info("Case {} auto-assigned to court-based officer {} (roleId: {}, roleCode: {}) based on state {}. Court: {}, Officer ID: {}",
                            instance.getCaseId(), p.getOfficerId(), roleId, roleCode, state.getStateCode(), caseEntity.getCourtId(), p.getOfficerId());
                    log.debug("=== AUTO-ASSIGNMENT DEBUG END (SUCCESS - COURT_BASED) ===");
                    return;
                }
                log.warn("No court-based officer found for roleId {} (roleCode: {}) at court {} (case {}). Available roles: {}",
                        roleId, roleCode, caseEntity.getCourtId(), instance.getCaseId(),
                        allCourtPostings.stream().map(OfficerDaHistory::getRoleCode).distinct().toList());
            }
        }

        if (!roleIdsForState.isEmpty()) {
            Long expectedRoleId = roleIdsForState.get(0);
            instance.setAssignedToRoleId(expectedRoleId);
            RoleMaster expectedRole = roleMasterRepository.findById(expectedRoleId).orElse(null);
            if (expectedRole != null) {
                instance.setAssignedToRoleRef(expectedRole);
                instance.setAssignedToRole(expectedRole.getRoleCode());
            }
            String expectedCode = expectedRole != null ? expectedRole.getRoleCode() : null;
            boolean isUnitBasedRole = expectedCode != null && unitBasedRoles.contains(expectedCode);
            if (isUnitBasedRole) {
                log.warn("Case {} cannot be auto-assigned. Expected unit-based roleId: {} (roleCode: {}) but no officer posted to unit {} with this role.",
                        instance.getCaseId(), expectedRoleId, expectedCode, caseUnitId);
            } else {
                log.warn("Case {} cannot be auto-assigned. Expected court-based roleId: {} (roleCode: {}) but no officer posted to court {} with this role. Available roles at court: {}",
                        instance.getCaseId(), expectedRoleId, expectedCode, caseEntity.getCourtId(),
                        allCourtPostings.stream().map(OfficerDaHistory::getRoleCode).distinct().toList());
            }
        }
        log.debug("=== AUTO-ASSIGNMENT DEBUG END (NO OFFICER FOUND) ===");
    }

    /**
     * Get role IDs (role_master) that have permissions to perform transitions from a state
     */
    private List<Long> getRoleIdsForState(Long workflowId, Long stateId) {
        List<Long> roleIds = new ArrayList<>();
        if (workflowId == null || stateId == null) {
            log.warn("  [getRoleIdsForState] Invalid parameters - Workflow ID: {}, State ID: {}", workflowId, stateId);
            return roleIds;
        }
        log.debug("  [getRoleIdsForState] Workflow ID: {}, State ID: {}", workflowId, stateId);
        
        // DEBUG: Check all transitions in the workflow to see what exists
        List<WorkflowTransition> allWorkflowTransitions = transitionRepository.findByWorkflowId(workflowId);
        log.debug("  [getRolesForState] Total transitions in workflow {}: {}", workflowId, allWorkflowTransitions.size());
        for (WorkflowTransition t : allWorkflowTransitions) {
            log.debug("  [getRolesForState] DB Transition - ID: {}, Code: {}, FromStateId: {}, ToStateId: {}, WorkflowId: {}, Active: {}", 
                    t.getId(), t.getTransitionCode(), t.getFromStateId(), t.getToStateId(), t.getWorkflowId(), t.getIsActive());
        }
        
        // DEBUG: Check transitions by fromStateId only (without workflow filter)
        List<WorkflowTransition> transitionsByState = transitionRepository
                .findByFromStateIdAndIsActiveTrue(stateId);
        log.debug("  [getRolesForState] Transitions FROM state ID {} (any workflow): {}", stateId, transitionsByState.size());
        for (WorkflowTransition t : transitionsByState) {
            log.debug("  [getRolesForState] Found transition - ID: {}, Code: {}, WorkflowId: {}, FromStateId: {}, Active: {}", 
                    t.getId(), t.getTransitionCode(), t.getWorkflowId(), t.getFromStateId(), t.getIsActive());
        }
        
        // Get all transitions FROM this state (with workflow filter)
        List<WorkflowTransition> transitions = transitionRepository
                .findTransitionsFromState(workflowId, stateId);
        
        log.debug("  [getRolesForState] Found {} transition(s) FROM state ID {} in workflow {} (with workflow filter)", 
                transitions.size(), stateId, workflowId);
        
        if (transitions.isEmpty()) {
            log.warn("  [getRolesForState] ⚠️ NO TRANSITIONS FOUND FROM state ID {} in workflow {}.", stateId, workflowId);
            if (!transitionsByState.isEmpty()) {
                log.warn("  [getRolesForState] ⚠️ BUT found {} transition(s) FROM state ID {} in OTHER workflows! " +
                        "Check if workflowId is correct. Expected: {}, Found in transitions: {}", 
                        transitionsByState.size(), stateId, workflowId, 
                        transitionsByState.stream().map(t -> t.getWorkflowId()).distinct().toList());
            } else {
                log.warn("  [getRolesForState] ⚠️ AND no transitions found FROM state ID {} in ANY workflow! " +
                        "This is why no roles are found! Create transitions FROM this state.", stateId);
            }
            return roleIds;
        }
        for (WorkflowTransition transition : transitions) {
            if (!transition.getIsActive()) continue;
            List<WorkflowPermission> permissions = permissionRepository.findByTransitionIdAndIsActiveTrue(transition.getId());
            for (WorkflowPermission permission : permissions) {
                if (!permission.getCanInitiate()) continue;
                Long rid = permission.getRoleId();
                if (rid == null && permission.getRoleCode() != null) {
                    rid = roleMasterRepository.findByRoleCode(permission.getRoleCode()).map(RoleMaster::getId).orElse(null);
                }
                if (rid != null && !roleIds.contains(rid)) {
                    roleIds.add(rid);
                    log.debug("  [getRoleIdsForState] Added roleId: {} (roleCode: {})", rid, permission.getRoleCode());
                }
            }
        }
        log.debug("  [getRoleIdsForState] Final roleIds list: {}", roleIds);
        return roleIds;
    }

    /**
     * Get checklist status for a transition on a specific case
     * Shows which conditions are met and which are blocking
     */
    @Transactional(readOnly = true)
    public TransitionChecklistDTO getTransitionChecklist(Long caseId, String transitionCode, Long officerId, Long roleId, String roleCode, Long unitId) {
        log.info("[GET_CHECKLIST] START: caseId={}, transitionCode={}, officerId={}, roleId={}, roleCode={}, unitId={}",
                caseId, transitionCode, officerId, roleId, roleCode, unitId);

        // Get workflow instance
        CaseWorkflowInstance instance = instanceRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Workflow instance not found for case: " + caseId));

        // Get current state
        WorkflowState currentState = instance.getCurrentState();
        if (currentState == null) {
            throw new RuntimeException("Current state not found for case: " + caseId);
        }

        // Get transition: filter transitions FROM current state by code to avoid non-unique results
        List<WorkflowTransition> candidateTransitions = transitionRepository
                .findTransitionsFromState(instance.getWorkflowId(), currentState.getId())
                .stream()
                .filter(t -> transitionCode.equals(t.getTransitionCode()))
                .toList();
        if (candidateTransitions.isEmpty()) {
            throw new RuntimeException("Transition not found from current state for code: " + transitionCode);
        }
        if (candidateTransitions.size() > 1) {
            log.warn("Multiple transitions found for checklist: workflowId={}, fromStateId={}, transitionCode={}. Using first.",
                    instance.getWorkflowId(), currentState.getId(), transitionCode);
        }
        WorkflowTransition transition = candidateTransitions.get(0);

        // Get case
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        // Get unit level
        Long unitIdValue = unitId;
        AdminUnit unit = null;
        if (unitIdValue != null) {
            unit = adminUnitRepository.findById(unitIdValue)
                    .orElse(null);
        }

        List<WorkflowPermission> permissions = new ArrayList<>();
        if (roleId != null && roleId != 0L && unit != null) {
            permissions = permissionRepository.findPermissionsForTransitionAndRoleId(transition.getId(), roleId, unit.getUnitLevel());
            if (permissions.isEmpty()) {
                permissions = permissionRepository.findPermissionsForTransitionAndRoleIdAnyLevel(transition.getId(), roleId);
            }
        }
        if (permissions.isEmpty() && roleCode != null && unit != null) {
            permissions = permissionRepository.findPermissionsForTransitionAndRole(transition.getId(), roleCode, unit.getUnitLevel());
        }
        if (permissions.isEmpty() && roleCode != null) {
            permissions = permissionRepository.findPermissionsForTransitionAndRoleAnyLevel(transition.getId(), roleCode);
        }

        // Build checklist from all permissions' conditions
        List<ConditionStatusDTO> allConditions = new ArrayList<>();
        boolean canExecute = false;

        for (WorkflowPermission permission : permissions) {
            if (!permission.getCanInitiate() || !permission.getIsActive()) {
                log.debug("[GET_CHECKLIST] Skipping permission {} - canInitiate={}, isActive={}", 
                        permission.getId(), permission.getCanInitiate(), permission.getIsActive());
                continue;
            }

            // Check hierarchy rule
            boolean hierarchyPassed = checkHierarchyRule(permission, unitId, instance);
            if (!hierarchyPassed) {
                log.debug("[GET_CHECKLIST] Permission {} failed hierarchy check", permission.getId());
                continue;
            }

            // Parse and check conditions
            log.info("[GET_CHECKLIST] Evaluating conditions for permission {} (caseId: {}, transitionCode: {})", 
                    permission.getId(), caseId, transitionCode);
            List<ConditionStatusDTO> conditionStatuses = new ArrayList<>();
            try {
                conditionStatuses = evaluateConditions(permission, instance, caseEntity);
                log.info("[GET_CHECKLIST] Evaluated {} condition(s) for permission {} (caseId: {}, transitionCode: {})", 
                        conditionStatuses.size(), permission.getId(), caseId, transitionCode);
                allConditions.addAll(conditionStatuses);
            } catch (Exception e) {
                log.error("[GET_CHECKLIST] ERROR evaluating conditions for permission {} (caseId: {}, transitionCode: {}): {}", 
                        permission.getId(), caseId, transitionCode, e.getMessage(), e);
                // Continue with other permissions - conditionStatuses will be empty
            }

            // If no conditions are defined, transition can be executed (no restrictions)
            // If conditions exist, all must pass
            if (conditionStatuses.isEmpty()) {
                // No conditions = allowed by default
                canExecute = true;
            } else {
                // Check if all conditions pass
                boolean allConditionsPassed = conditionStatuses.stream()
                        .allMatch(ConditionStatusDTO::getPassed);
                if (allConditionsPassed) {
                    canExecute = true;
                }
            }
        }

        // Remove duplicates (same condition from multiple permissions).
        // Include label in key so multiple document/form conditions (different template ids or form ids) are not merged.
        Map<String, ConditionStatusDTO> uniqueConditions = new HashMap<>();
        for (ConditionStatusDTO condition : allConditions) {
            String key = condition.getType() + "_"
                    + (condition.getFlagName() != null ? condition.getFlagName() : "")
                    + "_" + (condition.getModuleType() != null ? condition.getModuleType() : "")
                    + "_" + (condition.getFieldName() != null ? condition.getFieldName() : "")
                    + "_" + (condition.getLabel() != null ? condition.getLabel() : "");
            if (!uniqueConditions.containsKey(key) || !Boolean.TRUE.equals(condition.getPassed())) {
                uniqueConditions.put(key, condition);
            }
        }

        List<ConditionStatusDTO> finalConditions = new ArrayList<>(uniqueConditions.values());
        List<String> blockingReasons = finalConditions.stream()
                .filter(c -> !c.getPassed() && c.getRequired())
                .map(ConditionStatusDTO::getMessage)
                .distinct()
                .collect(Collectors.toList());

        // Include permission document/form options from first matching permission so UI can show what is required
        List<Long> allowedFormIds = null;
        List<Long> allowedDocumentIds = null;
        Boolean allowDocumentDraft = null;
        Boolean allowDocumentSaveAndSign = null;
        for (WorkflowPermission permission : permissions) {
            if (Boolean.TRUE.equals(permission.getCanInitiate()) && Boolean.TRUE.equals(permission.getIsActive())
                    && checkHierarchyRule(permission, unitId, instance)) {
                Map<String, Object> opts = extractPermissionOptionsFromConditions(permission.getConditions());
                if (opts != null) {
                    if (opts.containsKey("allowedFormIds")) allowedFormIds = toLongList(opts.get("allowedFormIds"));
                    if (opts.containsKey("allowedDocumentIds")) allowedDocumentIds = toLongList(opts.get("allowedDocumentIds"));
                    if (opts.containsKey("allowDocumentDraft")) allowDocumentDraft = toBoolean(opts.get("allowDocumentDraft"));
                    if (opts.containsKey("allowDocumentSaveAndSign")) allowDocumentSaveAndSign = toBoolean(opts.get("allowDocumentSaveAndSign"));
                }
                break;
            }
        }

        TransitionChecklistDTO result = TransitionChecklistDTO.builder()
                .transitionCode(transition.getTransitionCode())
                .transitionName(transition.getTransitionName())
                .canExecute(canExecute)
                .conditions(finalConditions)
                .blockingReasons(blockingReasons)
                .allowedFormIds(allowedFormIds)
                .allowedDocumentIds(allowedDocumentIds)
                .allowDocumentDraft(allowDocumentDraft)
                .allowDocumentSaveAndSign(allowDocumentSaveAndSign)
                .build();
        
        log.info("[GET_CHECKLIST] END: caseId={}, transitionCode={}, canExecute={}, conditionsCount={}", 
                caseId, transitionCode, canExecute, finalConditions.size());
        return result;
    }

    /** Extract allowedFormIds, allowedDocumentIds, allowDocumentDraft, allowDocumentSaveAndSign from permission conditions JSON. */
    private Map<String, Object> extractPermissionOptionsFromConditions(String conditionsJson) {
        if (conditionsJson == null || conditionsJson.trim().isEmpty()) return null;
        try {
            return objectMapper.readValue(conditionsJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.debug("Could not parse permission conditions: {}", e.getMessage());
            return null;
        }
    }

    private static List<Long> toLongList(Object value) {
        if (value == null) return null;
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(o -> o instanceof Number n ? n.longValue() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return null;
    }

    private static Boolean toBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return Boolean.parseBoolean(s);
        return null;
    }

    /**
     * Evaluate conditions and return detailed status for each
     */
    private List<ConditionStatusDTO> evaluateConditions(WorkflowPermission permission, 
                                                       CaseWorkflowInstance instance, 
                                                       Case caseEntity) {
        List<ConditionStatusDTO> conditions = new ArrayList<>();
        String conditionsJson = permission.getConditions();
        
        if (conditionsJson == null || conditionsJson.trim().isEmpty()) {
            return conditions;
        }

        try {
            Map<String, Object> conditionsMap = objectMapper.readValue(conditionsJson, 
                    new TypeReference<Map<String, Object>>() {});

            Map<String, Object> workflowData = parseJsonToMap(instance.getWorkflowData());
            Map<String, Object> caseData = parseJsonToMap(caseEntity.getCaseData());

            // Check workflow flags
            if (conditionsMap.containsKey("workflowDataFieldsRequired")) {
                List<String> requiredFlags = castToStringList(conditionsMap.get("workflowDataFieldsRequired"));
                for (String flag : requiredFlags) {
                    boolean passed = workflowData.containsKey(flag) && 
                            Boolean.TRUE.equals(workflowData.get(flag));
                    String label = getFlagDisplayLabel(flag);
                    
                    // Detect if flag indicates a form requirement (e.g., HEARING_SUBMITTED means HEARING form)
                    String moduleType = extractModuleTypeFromFlag(flag);
                    
                    ConditionStatusDTO.ConditionStatusDTOBuilder builder = ConditionStatusDTO.builder()
                            .label(label)
                            .type("WORKFLOW_FLAG")
                            .flagName(flag)
                            .required(true)
                            .passed(passed)
                            .message(passed ? label + " ✓" : label + " must be completed");
                    
                    // Set moduleType if detected (helps frontend show the correct form)
                    if (moduleType != null) {
                        builder.moduleType(moduleType);
                        // Also set type to FORM_FIELD for better frontend handling
                        builder.type("FORM_FIELD");
                    }
                    
                    conditions.add(builder.build());
                }
            }

            // Check module form fields
            if (conditionsMap.containsKey("moduleFormFieldsRequired")) {
                List<Map<String, Object>> moduleFields = castToMapList(conditionsMap.get("moduleFormFieldsRequired"));
                for (Map<String, Object> fieldReq : moduleFields) {
                    String moduleTypeStr = String.valueOf(fieldReq.get("moduleType"));
                    String fieldName = String.valueOf(fieldReq.get("fieldName"));
                    
                    try {
                        ModuleType moduleType = ModuleType.valueOf(moduleTypeStr);
                        boolean passed = checkModuleFormField(instance.getCaseId(), moduleType, fieldName);
                        String label = getModuleFieldDisplayLabel(moduleType, fieldName);
                        conditions.add(ConditionStatusDTO.builder()
                                .label(label)
                                .type("FORM_FIELD")
                                .moduleType(moduleTypeStr)
                                .fieldName(fieldName)
                                .required(true)
                                .passed(passed)
                                .message(passed ? label + " ✓" : label + " must be filled")
                                .build());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid module type: {}", moduleTypeStr);
                    }
                }
            }

            // Check case data fields
            if (conditionsMap.containsKey("caseDataFieldsRequired")) {
                List<String> requiredFields = castToStringList(conditionsMap.get("caseDataFieldsRequired"));
                for (String field : requiredFields) {
                    boolean passed = hasRequiredFields(caseData, List.of(field));
                    String label = getCaseFieldDisplayLabel(field);
                    conditions.add(ConditionStatusDTO.builder()
                            .label(label)
                            .type("CASE_DATA_FIELD")
                            .fieldName(field)
                            .required(true)
                            .passed(passed)
                            .message(passed ? label + " ✓" : label + " must be filled")
                            .build());
                }
            }

            // Check case data field equals
            if (conditionsMap.containsKey("caseDataFieldEquals")) {
                Map<String, Object> fieldEquals = castToMap(conditionsMap.get("caseDataFieldEquals"));
                for (Map.Entry<String, Object> entry : fieldEquals.entrySet()) {
                    String field = entry.getKey();
                    Object expected = entry.getValue();
                    Object actual = caseData.get(field);
                    boolean passed = actual != null && actual.toString().equals(expected.toString());
                    String label = getCaseFieldDisplayLabel(field) + " = " + expected;
                    conditions.add(ConditionStatusDTO.builder()
                            .label(label)
                            .type("CASE_DATA_FIELD")
                            .fieldName(field)
                            .required(true)
                            .passed(passed)
                            .message(passed ? label + " ✓" : label + " required")
                            .build());
                }
            }

            // Document conditions based on allowedDocumentIds and stage flags (allowDocumentDraft / allowDocumentSaveAndSign)
            if (conditionsMap.containsKey("allowedDocumentIds")) {
                log.info("[EVALUATE_CONDITIONS] Checking document conditions for caseId={}, permissionId={}", 
                        caseEntity.getId(), permission.getId());
                List<Long> templateIds = toLongList(conditionsMap.get("allowedDocumentIds"));
                if (templateIds != null && !templateIds.isEmpty()) {
                    log.info("[EVALUATE_CONDITIONS] Document templateIds: {} for caseId={}", templateIds, caseEntity.getId());
                    Boolean requireDraft = toBoolean(conditionsMap.get("allowDocumentDraft"));
                    Boolean requireSigned = toBoolean(conditionsMap.get("allowDocumentSaveAndSign"));

                    List<DocumentStatus> requiredStatuses = new ArrayList<>();
                    if (Boolean.TRUE.equals(requireDraft)) {
                        requiredStatuses.add(DocumentStatus.DRAFT);
                    }
                    if (Boolean.TRUE.equals(requireSigned)) {
                        requiredStatuses.add(DocumentStatus.SIGNED);
                    }

                    if (!requiredStatuses.isEmpty()) {
                        log.info("[EVALUATE_CONDITIONS] Checking document existence: caseId={}, templateIds={}, requiredStatuses={}", 
                                caseEntity.getId(), templateIds, requiredStatuses);
                        try {
                            boolean passed = caseDocumentRepository.existsByCaseIdAndTemplateIdInAndStatusIn(
                                    caseEntity.getId(), templateIds, requiredStatuses);
                            log.info("[EVALUATE_CONDITIONS] Document check result: passed={} for caseId={}, templateIds={}", 
                                    passed, caseEntity.getId(), templateIds);

                            String stageLabel;
                            if (Boolean.TRUE.equals(requireDraft) && Boolean.TRUE.equals(requireSigned)) {
                                stageLabel = "Draft or Signed";
                            } else if (Boolean.TRUE.equals(requireDraft)) {
                                stageLabel = "Draft";
                            } else {
                                stageLabel = "Signed";
                            }

                            String documentNamesLabel = getDocumentNamesLabel(templateIds);

                            // Derive module type from first template (if all templates share same module type).
                            String documentModuleType = null;
                            try {
                                List<CaseDocumentTemplate> templates = caseDocumentTemplateRepository.findAllById(templateIds);
                                if (!templates.isEmpty() && templates.get(0).getModuleType() != null) {
                                    documentModuleType = templates.get(0).getModuleType().name();
                                    log.debug("[EVALUATE_CONDITIONS] Document module type: {} for templateIds: {}", 
                                            documentModuleType, templateIds);
                                }
                            } catch (Exception e) {
                                log.warn("[EVALUATE_CONDITIONS] Could not resolve document module type for templateIds {}: {}", 
                                        templateIds, e.getMessage());
                            }

                            String label = "Document(s) [" + documentNamesLabel + "] " + stageLabel + " must exist";
                            conditions.add(ConditionStatusDTO.builder()
                                    .label(label)
                                    .type("DOCUMENT_CONDITION")
                                    .moduleType(documentModuleType)
                                    .documentTemplateIds(templateIds)
                                    .required(true)
                                    .passed(passed)
                                    .message(passed ? label + " ✓" : label + " required")
                                    .build());
                        } catch (Exception e) {
                            log.error("[EVALUATE_CONDITIONS] ERROR checking document conditions for caseId={}, templateIds={}: {}", 
                                    caseEntity.getId(), templateIds, e.getMessage(), e);
                            // Don't add condition if check fails - continue with other conditions
                        }
                    }
                }
            }

            // Form conditions from allowedFormIds (permission-forms list): each required form must be submitted
            if (conditionsMap.containsKey("allowedFormIds")) {
                List<Long> formIds = toLongList(conditionsMap.get("allowedFormIds"));
                if (formIds != null && !formIds.isEmpty()) {
                    Long caseNatureId = caseEntity.getCaseNatureId();
                    Long caseTypeId = caseEntity.getCaseTypeId();
                    for (Long formId : formIds) {
                        Optional<CaseModuleFormFieldDefinition> fieldOpt = caseModuleFormFieldRepository.findById(formId);
                        if (fieldOpt.isEmpty()) {
                            log.debug("Form id {} not found in case_module_form_fields, skipping form condition", formId);
                            continue;
                        }
                        CaseModuleFormFieldDefinition field = fieldOpt.get();
                        if (!field.getCaseNatureId().equals(caseNatureId)) {
                            continue;
                        }
                        if (field.getCaseTypeId() != null && !field.getCaseTypeId().equals(caseTypeId)) {
                            continue;
                        }
                        ModuleType moduleType = field.getModuleType();
                        String flag = moduleType.name() + "_SUBMITTED";
                        boolean passed = workflowData.containsKey(flag) && Boolean.TRUE.equals(workflowData.get(flag));
                        String label = "Form [" + moduleType.name() + "] must be submitted";
                        conditions.add(ConditionStatusDTO.builder()
                                .label(label)
                                .type("FORM_CONDITION")
                                .moduleType(moduleType.name())
                                .formId(formId)
                                .required(true)
                                .passed(passed)
                                .message(passed ? label + " ✓" : label + " must be submitted")
                                .build());
                    }
                }
            }

        } catch (Exception e) {
            log.error("[EVALUATE_CONDITIONS] ERROR evaluating conditions for permission {} (caseId: {}): {}", 
                    permission.getId(), caseEntity.getId(), e.getMessage(), e);
            // Return partial conditions - don't throw exception
        }

        log.info("[EVALUATE_CONDITIONS] END: permissionId={}, caseId={}, conditionsCount={}", 
                permission.getId(), caseEntity.getId(), conditions.size());
        return conditions;
    }

    /**
     * Check if a module form field has a value
     */
    private boolean checkModuleFormField(Long caseId, ModuleType moduleType, String fieldName) {
        Optional<CaseModuleFormSubmission> submission = moduleFormSubmissionRepository
                .findTopByCaseIdAndModuleTypeOrderBySubmittedAtDesc(caseId, moduleType);
        
        if (submission.isEmpty()) {
            return false;
        }

        Map<String, Object> formData = parseJsonToMap(submission.get().getFormData());
        Object value = formData.get(fieldName);
        return value != null && !value.toString().trim().isEmpty();
    }

    /**
     * Get display label for workflow flag (single source: WorkflowDataKey).
     */
    private String getFlagDisplayLabel(String flagName) {
        return WorkflowDataKey.getDisplayLabel(flagName);
    }

    /**
     * Get display label for module field
     */
    private String getModuleFieldDisplayLabel(ModuleType moduleType, String fieldName) {
        String moduleName = moduleType.name().toLowerCase();
        moduleName = moduleName.substring(0, 1).toUpperCase() + moduleName.substring(1);
        return moduleName + " - " + fieldName.replaceAll("([A-Z])", " $1").trim();
    }

    /**
     * Get display label for case data field
     */
    private String getCaseFieldDisplayLabel(String fieldName) {
        return fieldName.replaceAll("([A-Z])", " $1").trim();
    }

    /**
     * Resolve document template IDs to names for checklist labels.
     * Preserves order of templateIds; uses "ID &lt;id&gt;" when template not found.
     */
    private String getDocumentNamesLabel(List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return "";
        }
        List<CaseDocumentTemplate> templates = caseDocumentTemplateRepository.findAllById(templateIds);
        Map<Long, String> idToName = templates.stream()
                .collect(Collectors.toMap(CaseDocumentTemplate::getId, t -> t.getTemplateName() != null ? t.getTemplateName() : ("ID " + t.getId()), (a, b) -> a));
        return templateIds.stream()
                .map(id -> idToName.getOrDefault(id, "ID " + id))
                .collect(Collectors.joining(", "));
    }

    /**
     * Extract module type from workflow flag name
     * Examples: HEARING_SUBMITTED -> HEARING, NOTICE_SIGNED -> NOTICE
     */
    private String extractModuleTypeFromFlag(String flagName) {
        if (flagName == null || flagName.isEmpty()) {
            return null;
        }
        
        // Check for known module types in flag names (NOTICE_DRAFT before NOTICE so NOTICE_DRAFT_* matches correctly)
        String[] moduleTypes = {"HEARING", "NOTICE_DRAFT", "NOTICE", "ORDERSHEET", "JUDGEMENT", "ATTENDANCE", "FIELD_REPORT_REQUEST", "SUBMIT_FIELD_REPORT"};
        
        for (String moduleType : moduleTypes) {
            if (flagName.startsWith(moduleType + "_")) {
                return moduleType;
            }
        }
        
        return null;
    }

    /**
     * Extract module type from checklist conditions
     * Looks for FORM_FIELD type conditions with moduleType set
     */
    private String extractModuleTypeFromChecklist(TransitionChecklistDTO checklist) {
        if (checklist == null || checklist.getConditions() == null) {
            return null;
        }
        
        // Look for FORM_FIELD type conditions
        for (ConditionStatusDTO condition : checklist.getConditions()) {
            if ("FORM_FIELD".equals(condition.getType()) && condition.getModuleType() != null) {
                return condition.getModuleType();
            }
        }
        
        return null;
    }

    /**
     * Cast to list of maps
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castToMapList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> (Map<String, Object>) item)
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}

