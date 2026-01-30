package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private final CaseModuleFormSubmissionRepository moduleFormSubmissionRepository;
    private final CaseDocumentRepository documentRepository;
    private final CaseModuleFormService caseModuleFormService;
    private final ObjectMapper objectMapper;

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

        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

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
            if (permission.getCanInitiate() &&
                    checkHierarchyRule(permission, unitId, instance) &&
                    checkConditions(permission, instance, caseEntity)) {
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
        
        // Get case entity to ensure it's loaded for assignment logic
        Long caseIdValue = caseId;
        if (caseIdValue == null) {
            throw new RuntimeException("Case ID cannot be null");
        }
        Case caseEntity = caseRepository.findById(caseIdValue)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseIdValue));
        
        // Automatically assign case based on new workflow state and permissions
        assignCaseBasedOnWorkflowState(instance, toState, caseEntity);
        
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

        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

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
                // Show transitions based on permissions and hierarchy only
                // Conditions are checked when executing, not when showing available transitions
                if (permission.getCanInitiate() &&
                        checkHierarchyRule(permission, unitId, instance)) {
                    
                    // Get checklist for this transition
                    TransitionChecklistDTO checklist = null;
                    ModuleFormSchemaDTO formSchema = null;
                    
                    try {
                        checklist = getTransitionChecklist(caseId, transition.getTransitionCode(), 
                                officerId, roleCode, unitId);
                        
                        // Extract module type from checklist conditions to determine if form is needed
                        String moduleTypeStr = extractModuleTypeFromChecklist(checklist);
                        if (moduleTypeStr != null) {
                            try {
                                ModuleType moduleType = ModuleType.valueOf(moduleTypeStr);
                                // Fetch form schema for this module type
                                formSchema = caseModuleFormService.getFormSchema(
                                        caseEntity.getCaseNatureId(), 
                                        caseEntity.getCaseTypeId(), 
                                        moduleType);
                            } catch (IllegalArgumentException e) {
                                log.warn("Invalid module type extracted from checklist: {}", moduleTypeStr);
                            } catch (Exception e) {
                                log.warn("Failed to fetch form schema for module type {}: {}", 
                                        moduleTypeStr, e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get checklist for transition {}: {}", 
                                transition.getTransitionCode(), e.getMessage());
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

            // caseTypeCodesAllowed
            if (conditions.containsKey("caseTypeCodesAllowed")) {
                List<String> allowed = castToStringList(conditions.get("caseTypeCodesAllowed"));
                String caseTypeCode = caseEntity.getCaseType() != null ? caseEntity.getCaseType().getTypeCode() : null;
                if (caseTypeCode == null || !allowed.contains(caseTypeCode)) {
                    return false;
                }
            }

            // casePriorityIn
            if (conditions.containsKey("casePriorityIn")) {
                List<String> allowed = castToStringList(conditions.get("casePriorityIn"));
                String priority = caseEntity.getPriority();
                if (priority == null || !allowed.contains(priority)) {
                    return false;
                }
            }

            Map<String, Object> caseData = parseJsonToMap(caseEntity.getCaseData());
            if (conditions.containsKey("caseDataFieldsRequired")) {
                List<String> requiredFields = castToStringList(conditions.get("caseDataFieldsRequired"));
                if (!hasRequiredFields(caseData, requiredFields)) {
                    return false;
                }
            }

            if (conditions.containsKey("caseDataFieldEquals")) {
                Map<String, Object> fieldEquals = castToMap(conditions.get("caseDataFieldEquals"));
                if (!matchesFieldEquals(caseData, fieldEquals)) {
                    return false;
                }
            }

            Map<String, Object> workflowData = parseJsonToMap(instance.getWorkflowData());
            if (conditions.containsKey("workflowDataFieldsRequired")) {
                List<String> requiredFields = castToStringList(conditions.get("workflowDataFieldsRequired"));
                if (!hasRequiredFields(workflowData, requiredFields)) {
                    return false;
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
        log.debug("Case ID: {}, State: {} (ID: {}), Workflow ID: {}, Court ID: {}", 
                instance.getCaseId(), state.getStateCode(), state.getId(), 
                instance.getWorkflowId(), caseEntity != null ? caseEntity.getCourtId() : "NULL");
        
        if (caseEntity == null || caseEntity.getCourtId() == null) {
            log.warn("Case {} has no court assigned. Cannot auto-assign to officer.", instance.getCaseId());
            log.debug("=== AUTO-ASSIGNMENT DEBUG END (NO COURT) ===");
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
        
        List<String> rolesForState = getRolesForState(instance.getWorkflowId(), state.getId());
        
        log.debug("Step 2: Found {} role(s) for state {}: {}", 
                rolesForState.size(), state.getStateCode(), rolesForState);
        
        if (rolesForState.isEmpty()) {
            log.warn("No roles found with permissions for state {} (ID: {}). Case {} will remain unassigned.", 
                    state.getStateCode(), state.getId(), instance.getCaseId());
            log.debug("=== AUTO-ASSIGNMENT DEBUG END (NO ROLES) ===");
            return;
        }

        // Debug: Check all officers posted to this court
        List<OfficerDaHistory> allPostings = postingRepository.findByCourtIdAndIsCurrentTrue(caseEntity.getCourtId());
        log.debug("Step 3: Checking officers posted to court {} (ID: {})...", 
                caseEntity.getCourtId(), caseEntity.getCourtId());
        log.debug("Total active postings at court {}: {}", caseEntity.getCourtId(), allPostings.size());
        for (OfficerDaHistory posting : allPostings) {
            log.debug("  - Officer ID: {}, Role: {}, UserID: {}", 
                    posting.getOfficerId(), posting.getRoleCode(), posting.getPostingUserid());
        }

        // Try to find officer for each role (in order)
        log.debug("Step 4: Searching for officers with roles: {}...", rolesForState);
        for (String roleCode : rolesForState) {
            log.debug("  Checking role: {} at court {}...", roleCode, caseEntity.getCourtId());
            Optional<OfficerDaHistory> posting = postingRepository
                    .findByCourtIdAndRoleCodeAndIsCurrentTrue(caseEntity.getCourtId(), roleCode);
            
            if (posting.isPresent()) {
                // Assign case to this officer
                instance.setAssignedToOfficer(posting.get().getOfficer());
                instance.setAssignedToOfficerId(posting.get().getOfficerId());
                instance.setAssignedToRole(roleCode);
                log.info("Case {} auto-assigned to officer {} (role: {}) based on state {}. " +
                         "Court: {}, Officer ID: {}", 
                        instance.getCaseId(), posting.get().getOfficerId(), roleCode, state.getStateCode(),
                        caseEntity.getCourtId(), posting.get().getOfficerId());
                log.debug("=== AUTO-ASSIGNMENT DEBUG END (SUCCESS) ===");
                return; // Successfully assigned
            } else {
                log.warn("No officer found for role {} at court {} (case {}). " +
                        "Available roles at this court: {}", 
                        roleCode, caseEntity.getCourtId(), instance.getCaseId(),
                        allPostings.stream().map(OfficerDaHistory::getRoleCode).distinct().toList());
            }
        }

        // No officer found - set expected role but leave unassigned
        if (!rolesForState.isEmpty()) {
            instance.setAssignedToRole(rolesForState.get(0)); // Set first expected role
            log.warn("Case {} cannot be auto-assigned. Expected role: {} but no officer posted to court {} with this role. " +
                    "Available roles at court: {}", 
                    instance.getCaseId(), rolesForState.get(0), caseEntity.getCourtId(),
                    allPostings.stream().map(OfficerDaHistory::getRoleCode).distinct().toList());
        }
        log.debug("=== AUTO-ASSIGNMENT DEBUG END (NO OFFICER FOUND) ===");
    }

    /**
     * Get roles that have permissions to perform transitions from a state
     */
    private List<String> getRolesForState(Long workflowId, Long stateId) {
        List<String> roles = new ArrayList<>();
        
        // Validate inputs
        if (workflowId == null || stateId == null) {
            log.warn("  [getRolesForState] Invalid parameters - Workflow ID: {}, State ID: {}", workflowId, stateId);
            return roles;
        }
        
        log.debug("  [getRolesForState] Workflow ID: {}, State ID: {}", workflowId, stateId);
        
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
            return roles;
        }
        
        // For each transition, get roles with permissions
        for (WorkflowTransition transition : transitions) {
            log.debug("  [getRolesForState] Checking transition: {} (ID: {}), Active: {}", 
                    transition.getTransitionCode(), transition.getId(), transition.getIsActive());
            
            if (!transition.getIsActive()) {
                log.debug("  [getRolesForState] Skipping inactive transition: {}", transition.getTransitionCode());
                continue;
            }
            
            List<WorkflowPermission> permissions = permissionRepository
                    .findByTransitionIdAndIsActiveTrue(transition.getId());
            
            log.debug("  [getRolesForState] Transition {} has {} permission(s)", 
                    transition.getTransitionCode(), permissions.size());
            
            if (permissions.isEmpty()) {
                log.warn("  [getRolesForState] Transition {} (ID: {}) has NO PERMISSIONS! " +
                        "Create permissions for this transition.", transition.getTransitionCode(), transition.getId());
            }
            
            for (WorkflowPermission permission : permissions) {
                log.debug("  [getRolesForState] Permission - Role: {}, CanInitiate: {}, IsActive: {}", 
                        permission.getRoleCode(), permission.getCanInitiate(), permission.getIsActive());
                
                if (permission.getCanInitiate() && !roles.contains(permission.getRoleCode())) {
                    roles.add(permission.getRoleCode());
                    log.debug("  [getRolesForState] Added role: {} to roles list", permission.getRoleCode());
                } else {
                    if (!permission.getCanInitiate()) {
                        log.debug("  [getRolesForState] Skipping role {} - canInitiate is FALSE", 
                                permission.getRoleCode());
                    } else {
                        log.debug("  [getRolesForState] Skipping role {} - already in list", 
                                permission.getRoleCode());
                    }
                }
            }
        }
        
        log.debug("  [getRolesForState] Final roles list: {}", roles);
        return roles;
    }

    /**
     * Get checklist status for a transition on a specific case
     * Shows which conditions are met and which are blocking
     */
    @Transactional(readOnly = true)
    public TransitionChecklistDTO getTransitionChecklist(Long caseId, String transitionCode, Long officerId, String roleCode, Long unitId) {
        log.debug("Getting checklist for transition: caseId={}, transitionCode={}, officerId={}, roleCode={}, unitId={}",
                caseId, transitionCode, officerId, roleCode, unitId);

        // Get workflow instance
        CaseWorkflowInstance instance = instanceRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Workflow instance not found for case: " + caseId));

        // Get transition
        WorkflowTransition transition = transitionRepository
                .findByWorkflowIdAndTransitionCode(instance.getWorkflowId(), transitionCode)
                .orElseThrow(() -> new RuntimeException("Transition not found: " + transitionCode));

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

        // Get permissions for this transition and role
        List<WorkflowPermission> permissions = new ArrayList<>();
        if (unit != null) {
            permissions = permissionRepository
                    .findPermissionsForTransitionAndRole(transition.getId(), roleCode, unit.getUnitLevel());
        }
        if (permissions.isEmpty()) {
            permissions = permissionRepository
                    .findPermissionsForTransitionAndRoleAnyLevel(transition.getId(), roleCode);
        }

        // Build checklist from all permissions' conditions
        List<ConditionStatusDTO> allConditions = new ArrayList<>();
        boolean canExecute = false;

        for (WorkflowPermission permission : permissions) {
            if (!permission.getCanInitiate() || !permission.getIsActive()) {
                continue;
            }

            // Check hierarchy rule
            boolean hierarchyPassed = checkHierarchyRule(permission, unitId, instance);
            if (!hierarchyPassed) {
                continue;
            }

            // Parse and check conditions
            List<ConditionStatusDTO> conditionStatuses = evaluateConditions(permission, instance, caseEntity);
            allConditions.addAll(conditionStatuses);

            // If all conditions pass, transition can be executed
            boolean allConditionsPassed = conditionStatuses.stream()
                    .allMatch(ConditionStatusDTO::getPassed);
            if (allConditionsPassed) {
                canExecute = true;
            }
        }

        // Remove duplicates (same condition from multiple permissions)
        Map<String, ConditionStatusDTO> uniqueConditions = new HashMap<>();
        for (ConditionStatusDTO condition : allConditions) {
            String key = condition.getType() + "_" + 
                    (condition.getFlagName() != null ? condition.getFlagName() : "") +
                    (condition.getModuleType() != null ? condition.getModuleType() : "") +
                    (condition.getFieldName() != null ? condition.getFieldName() : "");
            if (!uniqueConditions.containsKey(key) || !condition.getPassed()) {
                uniqueConditions.put(key, condition);
            }
        }

        List<ConditionStatusDTO> finalConditions = new ArrayList<>(uniqueConditions.values());
        List<String> blockingReasons = finalConditions.stream()
                .filter(c -> !c.getPassed() && c.getRequired())
                .map(ConditionStatusDTO::getMessage)
                .distinct()
                .collect(Collectors.toList());

        return TransitionChecklistDTO.builder()
                .transitionCode(transition.getTransitionCode())
                .transitionName(transition.getTransitionName())
                .canExecute(canExecute)
                .conditions(finalConditions)
                .blockingReasons(blockingReasons)
                .build();
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

        } catch (Exception e) {
            log.error("Error evaluating conditions: {}", e.getMessage(), e);
        }

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
     * Get display label for workflow flag
     */
    private String getFlagDisplayLabel(String flagName) {
        Map<String, String> labels = Map.of(
                "HEARING_SUBMITTED", "Hearing form submitted",
                "NOTICE_SUBMITTED", "Notice form submitted",
                "NOTICE_DRAFT_CREATED", "Draft notice created",
                "NOTICE_READY", "Notice document ready",
                "ORDERSHEET_DRAFT_CREATED", "Draft ordersheet created",
                "ORDERSHEET_READY", "Ordersheet document ready",
                "JUDGEMENT_DRAFT_CREATED", "Draft judgement created",
                "JUDGEMENT_READY", "Judgement document ready"
        );
        return labels.getOrDefault(flagName, flagName.replace("_", " "));
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
     * Extract module type from workflow flag name
     * Examples: HEARING_SUBMITTED -> HEARING, NOTICE_READY -> NOTICE
     */
    private String extractModuleTypeFromFlag(String flagName) {
        if (flagName == null || flagName.isEmpty()) {
            return null;
        }
        
        // Check for known module types in flag names
        String[] moduleTypes = {"HEARING", "NOTICE", "ORDERSHEET", "JUDGEMENT"};
        
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

