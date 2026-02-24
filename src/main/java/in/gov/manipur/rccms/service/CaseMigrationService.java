package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.entity.*;
import in.gov.manipur.rccms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Case Migration Service
 * Handles migration and updates of old cases to match current workflow configuration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseMigrationService {

    private final CaseRepository caseRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final CaseWorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;

    /**
     * Initialize workflow instances for cases that don't have one
     * This is useful when migrating old cases to use workflows
     * 
     * @return Migration result with counts
     */
    @Transactional
    public Map<String, Object> initializeWorkflowInstancesForOldCases() {
        log.info("Starting workflow instance initialization for old cases...");
        
        int processedCount = 0;
        int successCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        List<String> errors = new java.util.ArrayList<>();
        
        // Find all active cases
        List<Case> allCases = caseRepository.findAll().stream()
                .filter(Case::getIsActive)
                .toList();
        
        log.info("Found {} active cases to process", allCases.size());
        
        for (Case caseEntity : allCases) {
            processedCount++;
            
            try {
                // Check if case already has workflow instance
                if (workflowInstanceRepository.findByCaseId(caseEntity.getId()).isPresent()) {
                    skippedCount++;
                    log.debug("Case {} already has workflow instance. Skipping.", caseEntity.getId());
                    continue;
                }
                
                // Get case type
                CaseType caseType = caseTypeRepository.findById(caseEntity.getCaseTypeId())
                        .orElse(null);
                
                if (caseType == null) {
                    errors.add("Case " + caseEntity.getId() + ": CaseType not found");
                    errorCount++;
                    continue;
                }
                
                // Get workflow code
                String workflowCode = caseType.getWorkflowCode();
                if (workflowCode == null || workflowCode.isEmpty()) {
                    errors.add("Case " + caseEntity.getId() + ": No workflow code configured for case type " + caseType.getTypeCode());
                    skippedCount++;
                    continue;
                }
                
                // Get workflow definition
                WorkflowDefinition workflow = workflowDefinitionRepository.findByWorkflowCode(workflowCode)
                        .orElse(null);
                
                if (workflow == null) {
                    errors.add("Case " + caseEntity.getId() + ": Workflow not found: " + workflowCode);
                    errorCount++;
                    continue;
                }
                
                // Get initial state
                WorkflowState initialState = workflowStateRepository
                        .findByWorkflowIdAndIsInitialStateTrue(workflow.getId())
                        .orElse(null);
                
                if (initialState == null) {
                    errors.add("Case " + caseEntity.getId() + ": Initial state not found for workflow " + workflowCode);
                    errorCount++;
                    continue;
                }
                
                // Create workflow instance
                CaseWorkflowInstance instance = new CaseWorkflowInstance();
                instance.setCaseEntity(caseEntity);
                instance.setCaseId(caseEntity.getId());
                instance.setWorkflow(workflow);
                instance.setWorkflowId(workflow.getId());
                instance.setCurrentState(initialState);
                instance.setCurrentStateId(initialState.getId());
                instance.setAssignedToUnit(caseEntity.getUnit());
                instance.setAssignedToUnitId(caseEntity.getUnitId());
                
                // Don't auto-assign here - let admin assign manually if needed
                // assignCaseBasedOnWorkflowState(instance, initialState, caseEntity);
                
                workflowInstanceRepository.save(instance);
                
                // Update case status to initial state
                caseEntity.setStatus(initialState.getStateCode());
                caseRepository.save(caseEntity);
                
                successCount++;
                log.info("Initialized workflow instance for case {}: {} -> {}", 
                        caseEntity.getId(), workflowCode, initialState.getStateCode());
                
            } catch (Exception e) {
                errorCount++;
                String errorMsg = "Case " + caseEntity.getId() + ": " + e.getMessage();
                errors.add(errorMsg);
                log.error("Error initializing workflow instance for case {}: {}", 
                        caseEntity.getId(), e.getMessage(), e);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", processedCount);
        result.put("successCount", successCount);
        result.put("skippedCount", skippedCount);
        result.put("errorCount", errorCount);
        result.put("errors", errors);
        
        log.info("Workflow instance initialization completed. Processed: {}, Success: {}, Skipped: {}, Errors: {}", 
                processedCount, successCount, skippedCount, errorCount);
        
        return result;
    }

    /**
     * Reset cases to initial state based on current workflow configuration
     * Use this when you've changed the initial state and want to reset cases
     * 
     * @param caseIds Optional list of case IDs to reset. If null, resets all cases
     * @return Migration result with counts
     */
    @Transactional
    public Map<String, Object> resetCasesToInitialState(List<Long> caseIds) {
        log.info("Starting reset cases to initial state...");
        
        int processedCount = 0;
        int successCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        List<String> errors = new java.util.ArrayList<>();
        
        // Get cases to process
        List<Case> casesToProcess;
        if (caseIds != null && !caseIds.isEmpty()) {
            casesToProcess = caseRepository.findAllById(caseIds).stream()
                    .filter(Case::getIsActive)
                    .toList();
        } else {
            casesToProcess = caseRepository.findAll().stream()
                    .filter(Case::getIsActive)
                    .toList();
        }
        
        log.info("Found {} cases to process", casesToProcess.size());
        
        for (Case caseEntity : casesToProcess) {
            processedCount++;
            
            try {
                // Get workflow instance
                CaseWorkflowInstance instance = workflowInstanceRepository.findByCaseId(caseEntity.getId())
                        .orElse(null);
                
                if (instance == null) {
                    errors.add("Case " + caseEntity.getId() + ": No workflow instance found");
                    skippedCount++;
                    continue;
                }
                
                // Get workflow
                WorkflowDefinition workflow = instance.getWorkflow();
                if (workflow == null && instance.getWorkflowId() != null) {
                    workflow = workflowDefinitionRepository.findById(instance.getWorkflowId()).orElse(null);
                }
                
                if (workflow == null) {
                    errors.add("Case " + caseEntity.getId() + ": Workflow not found");
                    errorCount++;
                    continue;
                }
                
                // Get current initial state
                WorkflowState initialState = workflowStateRepository
                        .findByWorkflowIdAndIsInitialStateTrue(workflow.getId())
                        .orElse(null);
                
                if (initialState == null) {
                    errors.add("Case " + caseEntity.getId() + ": Initial state not found for workflow " + workflow.getWorkflowCode());
                    errorCount++;
                    continue;
                }
                
                // Check if already in initial state
                if (instance.getCurrentStateId() != null && 
                    instance.getCurrentStateId().equals(initialState.getId())) {
                    skippedCount++;
                    log.debug("Case {} already in initial state. Skipping.", caseEntity.getId());
                    continue;
                }
                
                // Reset to initial state
                instance.setCurrentState(initialState);
                instance.setCurrentStateId(initialState.getId());
                workflowInstanceRepository.save(instance);
                
                // Update case status
                caseEntity.setStatus(initialState.getStateCode());
                caseRepository.save(caseEntity);
                
                successCount++;
                log.info("Reset case {} to initial state: {} -> {}", 
                        caseEntity.getId(), workflow.getWorkflowCode(), initialState.getStateCode());
                
            } catch (Exception e) {
                errorCount++;
                String errorMsg = "Case " + caseEntity.getId() + ": " + e.getMessage();
                errors.add(errorMsg);
                log.error("Error resetting case {} to initial state: {}", 
                        caseEntity.getId(), e.getMessage(), e);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", processedCount);
        result.put("successCount", successCount);
        result.put("skippedCount", skippedCount);
        result.put("errorCount", errorCount);
        result.put("errors", errors);
        
        log.info("Reset to initial state completed. Processed: {}, Success: {}, Skipped: {}, Errors: {}", 
                processedCount, successCount, skippedCount, errorCount);
        
        return result;
    }
}
