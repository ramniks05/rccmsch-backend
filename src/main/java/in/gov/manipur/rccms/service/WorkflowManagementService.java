package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.constant.WorkflowDataKey;
import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.*;
import in.gov.manipur.rccms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Workflow Management Service
 * Handles CRUD operations for workflow configuration
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowManagementService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowPermissionRepository permissionRepository;
    private final CaseWorkflowInstanceRepository instanceRepository;
    private final CaseNatureRepository caseNatureRepository;
    private final RoleMasterRepository roleMasterRepository;
    private final ObjectMapper objectMapper;

    // ==================== Workflow Definition CRUD ====================

    /**
     * Create workflow definition
     */
    public WorkflowDefinition createWorkflow(CreateWorkflowDTO dto) {
        log.info("Creating workflow: {}", dto.getWorkflowCode());

        // Check if workflow code already exists
        if (workflowDefinitionRepository.existsByWorkflowCode(dto.getWorkflowCode())) {
            throw new RuntimeException("Workflow with code '" + dto.getWorkflowCode() + "' already exists");
        }

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setWorkflowCode(dto.getWorkflowCode());
        workflow.setWorkflowName(dto.getWorkflowName());
        workflow.setDescription(dto.getDescription());
        workflow.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        workflow.setVersion(1);
        workflow.setCreatedAt(LocalDateTime.now());
        workflow.setUpdatedAt(LocalDateTime.now());

        return workflowDefinitionRepository.save(workflow);
    }

    /**
     * Update workflow definition
     */
    public WorkflowDefinition updateWorkflow(Long workflowId, CreateWorkflowDTO dto) {
        log.info("Updating workflow: id={}", workflowId);

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));

        // Check if workflow code is being changed and if new code already exists
        if (!workflow.getWorkflowCode().equals(dto.getWorkflowCode())) {
            if (workflowDefinitionRepository.existsByWorkflowCode(dto.getWorkflowCode())) {
                throw new RuntimeException("Workflow with code '" + dto.getWorkflowCode() + "' already exists");
            }
        }

        workflow.setWorkflowCode(dto.getWorkflowCode());
        workflow.setWorkflowName(dto.getWorkflowName());
        workflow.setDescription(dto.getDescription());
        if (dto.getIsActive() != null) {
            workflow.setIsActive(dto.getIsActive());
        }
        workflow.setUpdatedAt(LocalDateTime.now());
        workflow.setVersion(workflow.getVersion() + 1);

        return workflowDefinitionRepository.save(workflow);
    }

    /**
     * Delete workflow definition (soft delete)
     */
    public void deleteWorkflow(Long workflowId) {
        log.info("Deleting workflow: id={}", workflowId);

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));

        // Check if workflow is linked to any case natures
        List<CaseNature> linkedCaseNatures = caseNatureRepository.findAll().stream()
                .filter(cn -> workflow.getWorkflowCode().equals(cn.getWorkflowCode()))
                .collect(Collectors.toList());

        if (!linkedCaseNatures.isEmpty()) {
            throw new RuntimeException("Cannot delete workflow. It is linked to case natures: " +
                    linkedCaseNatures.stream().map(CaseNature::getName).collect(Collectors.joining(", ")));
        }

        // Check if workflow has active instances
        List<CaseWorkflowInstance> instances = instanceRepository.findAll().stream()
                .filter(i -> workflow.getId().equals(i.getWorkflowId()))
                .collect(Collectors.toList());

        if (!instances.isEmpty()) {
            throw new RuntimeException("Cannot delete workflow. It has " + instances.size() + " active case instances");
        }

        workflow.setIsActive(false);
        workflow.setUpdatedAt(LocalDateTime.now());
        workflowDefinitionRepository.save(workflow);
    }

    // ==================== Workflow State CRUD ====================

    /**
     * Create workflow state
     */
    public WorkflowState createState(Long workflowId, CreateStateDTO dto) {
        log.info("Creating state: workflowId={}, stateCode={}", workflowId, dto.getStateCode());

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));

        // Check if state code already exists for this workflow
        if (workflowStateRepository.findByWorkflowIdAndStateCode(workflowId, dto.getStateCode()).isPresent()) {
            throw new RuntimeException("State with code '" + dto.getStateCode() + "' already exists in this workflow");
        }

        // If this is an initial state, unset other initial states
        if (dto.getIsInitialState() != null && dto.getIsInitialState()) {
            workflowStateRepository.findByWorkflowIdAndIsInitialStateTrue(workflowId)
                    .ifPresent(state -> {
                        state.setIsInitialState(false);
                        workflowStateRepository.save(state);
                    });
        }

        WorkflowState state = new WorkflowState();
        state.setWorkflow(workflow);
        state.setWorkflowId(workflowId);
        state.setStateCode(dto.getStateCode());
        state.setStateName(dto.getStateName());
        state.setStateOrder(dto.getStateOrder());
        state.setIsInitialState(dto.getIsInitialState() != null ? dto.getIsInitialState() : false);
        state.setIsFinalState(dto.getIsFinalState() != null ? dto.getIsFinalState() : false);
        state.setDescription(dto.getDescription());
        state.setCreatedAt(LocalDateTime.now());

        return workflowStateRepository.save(state);
    }

    /**
     * Update workflow state
     */
    public WorkflowState updateState(Long stateId, CreateStateDTO dto) {
        log.info("Updating state: id={}", stateId);

        WorkflowState state = workflowStateRepository.findById(stateId)
                .orElseThrow(() -> new RuntimeException("State not found: " + stateId));

        // Check if state code is being changed and if new code already exists
        if (!state.getStateCode().equals(dto.getStateCode())) {
            if (workflowStateRepository.findByWorkflowIdAndStateCode(state.getWorkflowId(), dto.getStateCode()).isPresent()) {
                throw new RuntimeException("State with code '" + dto.getStateCode() + "' already exists in this workflow");
            }
        }

        // If this is being set as initial state, unset other initial states
        if (dto.getIsInitialState() != null && dto.getIsInitialState() && !state.getIsInitialState()) {
            workflowStateRepository.findByWorkflowIdAndIsInitialStateTrue(state.getWorkflowId())
                    .ifPresent(s -> {
                        s.setIsInitialState(false);
                        workflowStateRepository.save(s);
                    });
        }

        state.setStateCode(dto.getStateCode());
        state.setStateName(dto.getStateName());
        state.setStateOrder(dto.getStateOrder());
        if (dto.getIsInitialState() != null) {
            state.setIsInitialState(dto.getIsInitialState());
        }
        if (dto.getIsFinalState() != null) {
            state.setIsFinalState(dto.getIsFinalState());
        }
        if (dto.getDescription() != null) {
            state.setDescription(dto.getDescription());
        }

        return workflowStateRepository.save(state);
    }

    /**
     * Delete workflow state
     */
    public void deleteState(Long stateId) {
        log.info("Deleting state: id={}", stateId);

        WorkflowState state = workflowStateRepository.findById(stateId)
                .orElseThrow(() -> new RuntimeException("State not found: " + stateId));

        // Check if state is used in any transitions
        List<WorkflowTransition> transitionsFrom = transitionRepository.findByFromStateIdAndIsActiveTrue(stateId);
        List<WorkflowTransition> transitionsTo = transitionRepository.findAll().stream()
                .filter(t -> stateId.equals(t.getToStateId()))
                .collect(Collectors.toList());

        if (!transitionsFrom.isEmpty() || !transitionsTo.isEmpty()) {
            throw new RuntimeException("Cannot delete state. It is used in " +
                    (transitionsFrom.size() + transitionsTo.size()) + " transition(s)");
        }

        // Check if state is used in any workflow instances
        List<CaseWorkflowInstance> instances = instanceRepository.findByCurrentStateId(stateId);
        if (!instances.isEmpty()) {
            throw new RuntimeException("Cannot delete state. It is currently used in " + instances.size() + " case instance(s)");
        }

        workflowStateRepository.delete(state);
    }

    // ==================== Workflow Transition CRUD ====================

    /**
     * Create workflow transition
     */
    public WorkflowTransition createTransition(Long workflowId, CreateTransitionDTO dto) {
        log.info("Creating transition: workflowId={}, transitionCode={}", workflowId, dto.getTransitionCode());

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));

        // Check if transition code already exists for this workflow
        if (transitionRepository.findByWorkflowIdAndTransitionCode(workflowId, dto.getTransitionCode()).isPresent()) {
            throw new RuntimeException("Transition with code '" + dto.getTransitionCode() + "' already exists in this workflow");
        }

        // Validate states exist and belong to this workflow
        WorkflowState fromState = workflowStateRepository.findById(dto.getFromStateId())
                .orElseThrow(() -> new RuntimeException("From state not found: " + dto.getFromStateId()));

        WorkflowState toState = workflowStateRepository.findById(dto.getToStateId())
                .orElseThrow(() -> new RuntimeException("To state not found: " + dto.getToStateId()));

        if (!fromState.getWorkflowId().equals(workflowId) || !toState.getWorkflowId().equals(workflowId)) {
            throw new RuntimeException("States must belong to the same workflow");
        }

        WorkflowTransition transition = new WorkflowTransition();
        transition.setWorkflow(workflow);
        transition.setWorkflowId(workflowId);
        transition.setFromState(fromState);
        transition.setFromStateId(fromState.getId());
        transition.setToState(toState);
        transition.setToStateId(toState.getId());
        transition.setTransitionCode(dto.getTransitionCode());
        transition.setTransitionName(dto.getTransitionName());
        transition.setRequiresComment(dto.getRequiresComment() != null ? dto.getRequiresComment() : false);
        transition.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        transition.setDescription(dto.getDescription());
        transition.setCreatedAt(LocalDateTime.now());

        return transitionRepository.save(transition);
    }

    /**
     * Update workflow transition
     */
    public WorkflowTransition updateTransition(Long transitionId, CreateTransitionDTO dto) {
        log.info("Updating transition: id={}", transitionId);

        WorkflowTransition transition = transitionRepository.findById(transitionId)
                .orElseThrow(() -> new RuntimeException("Transition not found: " + transitionId));

        // Check if transition code is being changed and if new code already exists
        if (!transition.getTransitionCode().equals(dto.getTransitionCode())) {
            if (transitionRepository.findByWorkflowIdAndTransitionCode(transition.getWorkflowId(), dto.getTransitionCode()).isPresent()) {
                throw new RuntimeException("Transition with code '" + dto.getTransitionCode() + "' already exists in this workflow");
            }
        }

        // Validate states exist and belong to this workflow
        WorkflowState fromState = workflowStateRepository.findById(dto.getFromStateId())
                .orElseThrow(() -> new RuntimeException("From state not found: " + dto.getFromStateId()));

        WorkflowState toState = workflowStateRepository.findById(dto.getToStateId())
                .orElseThrow(() -> new RuntimeException("To state not found: " + dto.getToStateId()));

        if (!fromState.getWorkflowId().equals(transition.getWorkflowId()) || 
            !toState.getWorkflowId().equals(transition.getWorkflowId())) {
            throw new RuntimeException("States must belong to the same workflow");
        }

        transition.setFromState(fromState);
        transition.setFromStateId(fromState.getId());
        transition.setToState(toState);
        transition.setToStateId(toState.getId());
        transition.setTransitionCode(dto.getTransitionCode());
        transition.setTransitionName(dto.getTransitionName());
        if (dto.getRequiresComment() != null) {
            transition.setRequiresComment(dto.getRequiresComment());
        }
        if (dto.getIsActive() != null) {
            transition.setIsActive(dto.getIsActive());
        }
        if (dto.getDescription() != null) {
            transition.setDescription(dto.getDescription());
        }

        return transitionRepository.save(transition);
    }

    /**
     * Delete workflow transition
     */
    public void deleteTransition(Long transitionId) {
        log.info("Deleting transition: id={}", transitionId);

        WorkflowTransition transition = transitionRepository.findById(transitionId)
                .orElseThrow(() -> new RuntimeException("Transition not found: " + transitionId));

        // Check if transition has permissions
        List<WorkflowPermission> permissions = permissionRepository.findByTransitionIdAndIsActiveTrue(transitionId);
        if (!permissions.isEmpty()) {
            throw new RuntimeException("Cannot delete transition. It has " + permissions.size() + " permission(s). Delete permissions first.");
        }

        // Check if transition is used in workflow history
        // Note: We don't delete history, but we can check if it's safe to delete
        transitionRepository.delete(transition);
    }

    // ==================== Workflow Permission CRUD ====================

    /**
     * Create workflow permission. Resolves role from roleId (preferred) or roleCode and saves role_id + role_code.
     */
    public WorkflowPermission createPermission(Long transitionId, CreatePermissionDTO dto) {
        log.info("Creating permission: transitionId={}, roleId={}, roleCode={}", transitionId, dto.getRoleId(), dto.getRoleCode());

        WorkflowTransition transition = transitionRepository.findById(transitionId)
                .orElseThrow(() -> new RuntimeException("Transition not found: " + transitionId));

        RoleMaster role = resolveRoleFromDto(dto);
        if (role == null) {
            throw new RuntimeException("Role not found: provide roleId or roleCode");
        }
        String roleCode = role.getRoleCode();

        if (permissionRepository.existsByTransitionIdAndRoleCodeAndUnitLevelAndIsActiveTrue(
                transitionId, roleCode, dto.getUnitLevel())) {
            throw new RuntimeException("Permission already exists for transition: " + transitionId +
                    ", role: " + roleCode + ", unitLevel: " + dto.getUnitLevel());
        }

        WorkflowPermission permission = new WorkflowPermission();
        permission.setTransition(transition);
        permission.setRole(role);
        permission.setRoleCode(roleCode);
        permission.setUnitLevel(dto.getUnitLevel());
        permission.setCanInitiate(dto.getCanInitiate() != null ? dto.getCanInitiate() : false);
        permission.setCanApprove(dto.getCanApprove() != null ? dto.getCanApprove() : false);
        permission.setHierarchyRule(dto.getHierarchyRule());
        validateWorkflowDataFieldsInConditions(dto.getConditions());
        permission.setConditions(mergeConditionsWithPermissionOptions(dto, dto.getConditions()));
        permission.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        permission.setCreatedAt(LocalDateTime.now());

        return permissionRepository.save(permission);
    }

    /**
     * Update workflow permission. Updates role_id and role_code from roleId (preferred) or roleCode.
     */
    public WorkflowPermission updatePermission(Long permissionId, CreatePermissionDTO dto) {
        log.info("Updating permission: id={}, roleId={}, roleCode={}", permissionId, dto.getRoleId(), dto.getRoleCode());

        WorkflowPermission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionId));

        if (dto.getRoleId() != null || (dto.getRoleCode() != null && !dto.getRoleCode().isBlank())) {
            RoleMaster role = resolveRoleFromDto(dto);
            if (role == null) {
                throw new RuntimeException("Role not found: provide roleId or roleCode");
            }
            String roleCode = role.getRoleCode();
            if (!roleCode.equals(permission.getRoleCode()) || (dto.getUnitLevel() != null && !dto.getUnitLevel().equals(permission.getUnitLevel()))) {
                AdminUnit.UnitLevel newUnitLevel = dto.getUnitLevel() != null ? dto.getUnitLevel() : permission.getUnitLevel();
                if (permissionRepository.existsByTransitionIdAndRoleCodeAndUnitLevelAndIsActiveTrueAndIdNot(
                        permission.getTransitionId(), roleCode, newUnitLevel, permission.getId())) {
                    throw new RuntimeException("Permission already exists for this combination");
                }
            }
            permission.setRole(role);
            permission.setRoleCode(roleCode);
        }
        if (dto.getUnitLevel() != null) {
            permission.setUnitLevel(dto.getUnitLevel());
        }
        if (dto.getCanInitiate() != null) {
            permission.setCanInitiate(dto.getCanInitiate());
        }
        if (dto.getCanApprove() != null) {
            permission.setCanApprove(dto.getCanApprove());
        }
        if (dto.getHierarchyRule() != null) {
            permission.setHierarchyRule(dto.getHierarchyRule());
        }
        if (dto.getConditions() != null) {
            validateWorkflowDataFieldsInConditions(dto.getConditions());
        }
        String baseConditions = dto.getConditions() != null ? dto.getConditions() : permission.getConditions();
        permission.setConditions(mergeConditionsWithPermissionOptions(dto, baseConditions));
        if (dto.getIsActive() != null) {
            permission.setIsActive(dto.getIsActive());
        }

        return permissionRepository.save(permission);
    }

    /** Resolve RoleMaster from dto: by roleId if set, else by roleCode. */
    private RoleMaster resolveRoleFromDto(CreatePermissionDTO dto) {
        if (dto.getRoleId() != null) {
            return roleMasterRepository.findById(dto.getRoleId()).orElse(null);
        }
        if (dto.getRoleCode() != null && !dto.getRoleCode().isBlank()) {
            return roleMasterRepository.findByRoleCode(dto.getRoleCode().trim()).orElse(null);
        }
        return null;
    }

    /**
     * Validates that workflowDataFieldsRequired in conditions JSON only contains keys from
     * WorkflowDataKey (single source of truth). Use these same keys when writing workflow_data.
     */
    private void validateWorkflowDataFieldsInConditions(String conditionsJson) {
        if (conditionsJson == null || conditionsJson.isBlank()) {
            return;
        }
        try {
            Map<String, Object> conditions = objectMapper.readValue(conditionsJson, new TypeReference<Map<String, Object>>() {});
            Object req = conditions.get("workflowDataFieldsRequired");
            if (req == null) return;
            List<String> required = req instanceof List<?> list
                    ? list.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toList())
                    : List.of(req.toString());
            List<String> invalid = required.stream().filter(k -> !WorkflowDataKey.isValid(k)).collect(Collectors.toList());
            if (!invalid.isEmpty()) {
                throw new IllegalArgumentException(
                        "workflowDataFieldsRequired contains invalid keys: " + invalid + ". "
                                + "Use only keys from workflow data key reference. Valid keys: " + WorkflowDataKey.validKeys());
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            // JSON parse errors are not about key validity; let them surface as-is when saving
            log.debug("Could not parse conditions for workflowDataFieldsRequired validation: {}", e.getMessage());
        }
    }

    /**
     * Delete workflow permission
     */
    public void deletePermission(Long permissionId) {
        log.info("Deleting permission: id={}", permissionId);

        WorkflowPermission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + permissionId));

        permissionRepository.delete(permission);
    }

    // ==================== Helper Methods ====================

    /**
     * Convert WorkflowState to DTO
     */
    public WorkflowStateDTO convertToStateDTO(WorkflowState state) {
        WorkflowStateDTO dto = new WorkflowStateDTO();
        dto.setId(state.getId());
        dto.setWorkflowId(state.getWorkflowId());
        dto.setStateCode(state.getStateCode());
        dto.setStateName(state.getStateName());
        dto.setStateOrder(state.getStateOrder());
        dto.setIsInitialState(state.getIsInitialState());
        dto.setIsFinalState(state.getIsFinalState());
        dto.setDescription(state.getDescription());
        if (state.getWorkflow() != null) {
            dto.setWorkflowCode(state.getWorkflow().getWorkflowCode());
        }
        return dto;
    }

    /**
     * Convert WorkflowPermission to DTO
     */
    public WorkflowPermissionDTO convertToPermissionDTO(WorkflowPermission permission) {
        WorkflowPermissionDTO dto = new WorkflowPermissionDTO();
        dto.setId(permission.getId());
        dto.setTransitionId(permission.getTransitionId());
        dto.setRoleCode(permission.getRoleCode());
        dto.setUnitLevel(permission.getUnitLevel());
        dto.setCanInitiate(permission.getCanInitiate());
        dto.setCanApprove(permission.getCanApprove());
        dto.setHierarchyRule(permission.getHierarchyRule());
        dto.setConditions(permission.getConditions());
        dto.setIsActive(permission.getIsActive());
        if (permission.getTransition() != null) {
            dto.setTransitionCode(permission.getTransition().getTransitionCode());
        }
        parseConditionsIntoPermissionOptions(permission.getConditions(), dto);
        return dto;
    }

    /** Merge allowedFormIds, allowedDocumentIds, allowDocumentDraft, allowDocumentSaveAndSign into conditions JSON. */
    private String mergeConditionsWithPermissionOptions(CreatePermissionDTO dto, String baseConditionsJson) {
        Map<String, Object> map = new HashMap<>();
        if (baseConditionsJson != null && !baseConditionsJson.trim().isEmpty()) {
            try {
                map = objectMapper.readValue(baseConditionsJson, new TypeReference<Map<String, Object>>() {});
                if (map == null) map = new HashMap<>();
            } catch (Exception e) {
                log.warn("Could not parse conditions JSON, using empty base: {}", e.getMessage());
            }
        }
        // When lists are explicitly provided as empty (e.g. []), interpret that as
        // "no restriction" and remove the key from conditions.
        // Forms: if parameter is absent/null or empty, interpret as \"no form selected\"
        if (dto.getAllowedFormIds() == null || dto.getAllowedFormIds().isEmpty()) {
            map.remove("allowedFormIds");
        } else {
            map.put("allowedFormIds", dto.getAllowedFormIds());
        }
        if (dto.getAllowedDocumentIds() != null) {
            if (dto.getAllowedDocumentIds().isEmpty()) {
                map.remove("allowedDocumentIds");
            } else {
                map.put("allowedDocumentIds", dto.getAllowedDocumentIds());
            }
        }
        if (dto.getAllowDocumentDraft() != null) map.put("allowDocumentDraft", dto.getAllowDocumentDraft());
        if (dto.getAllowDocumentSaveAndSign() != null) map.put("allowDocumentSaveAndSign", dto.getAllowDocumentSaveAndSign());
        if (dto.getAllowedDocumentStages() != null) {
            if (dto.getAllowedDocumentStages().isEmpty()) {
                map.remove("allowedDocumentStages");
            } else {
                map.put("allowedDocumentStages", dto.getAllowedDocumentStages());
            }
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize conditions", e);
        }
    }

    /** Populate DTO with allowedFormIds, allowedDocumentIds, allowDocumentDraft, allowDocumentSaveAndSign from conditions JSON. */
    private void parseConditionsIntoPermissionOptions(String conditionsJson, WorkflowPermissionDTO dto) {
        if (conditionsJson == null || conditionsJson.trim().isEmpty()) return;
        try {
            Map<String, Object> map = objectMapper.readValue(conditionsJson, new TypeReference<Map<String, Object>>() {});
            if (map.containsKey("allowedFormIds")) dto.setAllowedFormIds(toLongList(map.get("allowedFormIds")));
            if (map.containsKey("allowedDocumentIds")) dto.setAllowedDocumentIds(toLongList(map.get("allowedDocumentIds")));
            if (map.containsKey("allowDocumentDraft")) dto.setAllowDocumentDraft(toBoolean(map.get("allowDocumentDraft")));
            if (map.containsKey("allowDocumentSaveAndSign")) dto.setAllowDocumentSaveAndSign(toBoolean(map.get("allowDocumentSaveAndSign")));
            if (map.containsKey("allowedDocumentStages")) dto.setAllowedDocumentStages(toAllowedDocumentStages(map.get("allowedDocumentStages")));
        } catch (Exception e) {
            log.debug("Could not parse permission options from conditions: {}", e.getMessage());
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

    private List<AllowedDocumentStageEntryDTO> toAllowedDocumentStages(Object value) {
        if (value == null) return null;
        if (value instanceof List<?> list && !list.isEmpty()) {
            try {
                return objectMapper.convertValue(value, new TypeReference<List<AllowedDocumentStageEntryDTO>>() {});
            } catch (Exception e) {
                log.debug("Could not convert allowedDocumentStages: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Get all conditions for a transition (aggregated from all permissions)
     */
    public TransitionConditionsDTO getTransitionConditions(Long transitionId) {
        log.debug("Getting conditions for transition: id={}", transitionId);

        WorkflowTransition transition = transitionRepository.findById(transitionId)
                .orElseThrow(() -> new RuntimeException("Transition not found: " + transitionId));

        // Get all permissions for this transition
        List<WorkflowPermission> permissions = permissionRepository.findByTransitionId(transitionId);

        List<TransitionConditionsDTO.PermissionConditionsDTO> permissionConditions = new ArrayList<>();

        for (WorkflowPermission permission : permissions) {
            Map<String, Object> conditionsMap = new HashMap<>();
            
            // Parse conditions JSON if present
            if (permission.getConditions() != null && !permission.getConditions().trim().isEmpty()) {
                try {
                    conditionsMap = objectMapper.readValue(
                            permission.getConditions(),
                            new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    log.warn("Failed to parse conditions JSON for permission {}: {}", 
                            permission.getId(), e.getMessage());
                    conditionsMap.put("_error", "Invalid JSON: " + e.getMessage());
                }
            }

            TransitionConditionsDTO.PermissionConditionsDTO permCond = 
                    TransitionConditionsDTO.PermissionConditionsDTO.builder()
                    .permissionId(permission.getId())
                    .roleCode(permission.getRoleCode())
                    .unitLevel(permission.getUnitLevel() != null ? permission.getUnitLevel().name() : null)
                    .hierarchyRule(permission.getHierarchyRule())
                    .conditions(conditionsMap)
                    .canInitiate(permission.getCanInitiate())
                    .isActive(permission.getIsActive())
                    .build();

            permissionConditions.add(permCond);
        }

        return TransitionConditionsDTO.builder()
                .transitionId(transition.getId())
                .transitionCode(transition.getTransitionCode())
                .transitionName(transition.getTransitionName())
                .permissions(permissionConditions)
                .build();
    }
}
