package in.gov.manipur.rccms.config;

import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.entity.RoleMaster;
import in.gov.manipur.rccms.entity.WorkflowPermission;
import in.gov.manipur.rccms.entity.WorkflowTransition;
import in.gov.manipur.rccms.repository.RoleMasterRepository;
import in.gov.manipur.rccms.repository.WorkflowPermissionRepository;
import in.gov.manipur.rccms.repository.WorkflowTransitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reader Permission Initializer
 * Adds READER permissions for case registration transitions (SUBMIT_APPLICATION)
 * This allows READER to accept/register cases from CITIZEN_APPLICATION state
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(4) // Initialize after workflows are created
public class ReaderPermissionInitializer implements CommandLineRunner {

    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowPermissionRepository permissionRepository;
    private final RoleMasterRepository roleMasterRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========================================");
        log.info("Initializing READER Permissions...");
        log.info("========================================");

        try {
            // Find all SUBMIT_APPLICATION transitions across all workflows
            List<WorkflowTransition> allTransitions = transitionRepository.findAll();
            List<WorkflowTransition> submitTransitions = allTransitions.stream()
                    .filter(t -> "SUBMIT_APPLICATION".equals(t.getTransitionCode()) && t.getIsActive())
                    .collect(Collectors.toList());

            if (submitTransitions.isEmpty()) {
                log.warn("No SUBMIT_APPLICATION transitions found. Skipping READER permission initialization.");
                return;
            }

            int addedCount = 0;
            int existingCount = 0;

            for (WorkflowTransition transition : submitTransitions) {
                // Check if READER permission already exists
                boolean exists = permissionRepository.existsPermissionForTransitionAndRole(
                        transition.getId(), "READER", AdminUnit.UnitLevel.DISTRICT);

                if (!exists) {
                    RoleMaster readerRole = roleMasterRepository.findByRoleCode("READER").orElse(null);
                    WorkflowPermission permission = new WorkflowPermission();
                    permission.setTransition(transition);
                    permission.setRoleCode("READER");
                    if (readerRole != null) permission.setRole(readerRole);
                    permission.setUnitLevel(AdminUnit.UnitLevel.DISTRICT);
                    permission.setCanInitiate(true);
                    permission.setCanApprove(false);
                    permission.setHierarchyRule("SAME_UNIT");
                    permission.setIsActive(true);
                    permission.setCreatedAt(LocalDateTime.now());
                    permissionRepository.save(permission);
                    
                    addedCount++;
                    log.info("Added READER permission for transition: {} (workflow: {})", 
                            transition.getTransitionCode(), 
                            transition.getWorkflow() != null ? transition.getWorkflow().getWorkflowCode() : "N/A");
                } else {
                    existingCount++;
                    log.debug("READER permission already exists for transition: {}", transition.getTransitionCode());
                }
            }

            log.info("========================================");
            log.info("READER Permission initialization completed!");
            log.info("Added: {}, Already existed: {}, Total transitions checked: {}", 
                    addedCount, existingCount, submitTransitions.size());
            log.info("========================================");
        } catch (Exception e) {
            log.error("Error initializing READER permissions: {}", e.getMessage(), e);
        }
    }
}
