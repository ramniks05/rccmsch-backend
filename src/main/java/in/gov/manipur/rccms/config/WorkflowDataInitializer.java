package in.gov.manipur.rccms.config;

import in.gov.manipur.rccms.entity.AdminUnit;
import in.gov.manipur.rccms.entity.WorkflowDefinition;
import in.gov.manipur.rccms.entity.WorkflowState;
import in.gov.manipur.rccms.entity.WorkflowTransition;
import in.gov.manipur.rccms.entity.WorkflowPermission;
import in.gov.manipur.rccms.repository.CaseNatureRepository;
import in.gov.manipur.rccms.repository.WorkflowDefinitionRepository;
import in.gov.manipur.rccms.repository.WorkflowStateRepository;
import in.gov.manipur.rccms.repository.WorkflowTransitionRepository;
import in.gov.manipur.rccms.repository.WorkflowPermissionRepository;
import in.gov.manipur.rccms.repository.RoleMasterRepository;
import in.gov.manipur.rccms.entity.RoleMaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Workflow Data Initializer
 * Automatically initializes all workflow definitions, states, transitions, and permissions
 * for all 9 case natures when application starts
 */
@Slf4j
// @Component // Disabled - Workflows will be created manually through admin interface
@RequiredArgsConstructor
@Order(3) // Initialize after roles and case natures
public class WorkflowDataInitializer implements CommandLineRunner {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowPermissionRepository permissionRepository;
    private final CaseNatureRepository caseNatureRepository;
    private final RoleMasterRepository roleMasterRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("========================================");
        log.info("Initializing Workflow Data...");
        log.info("========================================");

        try {
            initializeMutationGiftSaleWorkflow();
            initializeMutationDeathWorkflow();
            initializePartitionWorkflow();
            initializeClassificationBefore2014Workflow();
            initializeClassificationAfter2014Workflow();
            initializeCourtOrderWorkflow();
            initializeAllotmentWorkflow();
            initializeAcquisitionRFCTLARRWorkflow();
            initializeAcquisitionDirectPurchaseWorkflow();

            ensureApplicationSubmittedTransitions();

            log.info("========================================");
            log.info("Workflow Data initialization completed!");
            log.info("========================================");
        } catch (Exception e) {
            log.error("Error initializing workflow data: {}", e.getMessage(), e);
        }
    }

    /**
     * Ensure each workflow has an APPLICATION_SUBMITTED transition into the initial state
     * so that case submission can be recorded in history without an extra submit step.
     */
    private void ensureApplicationSubmittedTransitions() {
        for (WorkflowDefinition workflow : workflowDefinitionRepository.findAll()) {
            workflowStateRepository.findByWorkflowIdAndIsInitialStateTrue(workflow.getId())
                    .ifPresent(initialState -> {
                        List<WorkflowTransition> intoInitial = transitionRepository
                                .findByWorkflowIdAndToStateIdAndIsActiveTrue(workflow.getId(), initialState.getId());
                        if (!intoInitial.isEmpty()) return;
                        if (transitionRepository.findByWorkflowIdAndTransitionCode(workflow.getId(), "APPLICATION_SUBMITTED").isPresent())
                            return;
                        WorkflowTransition t = new WorkflowTransition();
                        t.setWorkflow(workflow);
                        t.setWorkflowId(workflow.getId());
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
                        transitionRepository.save(t);
                        log.debug("Created APPLICATION_SUBMITTED transition for workflow {}", workflow.getWorkflowCode());
                    });
        }
    }

    /**
     * I. Mutation (after Gift/Sale Deeds) Workflow (workflow_id=1)
     * Only workflow definition is created. States and transitions are managed by admin.
     */
    private void initializeMutationGiftSaleWorkflow() {
        String workflowCode = "MUTATION_GIFT_SALE";
        log.info("Initializing workflow: {} (definition only; states/transitions to be created via admin)", workflowCode);

        createOrGetWorkflow(
            workflowCode,
            "Mutation (after Gift/Sale Deeds)",
            "Workflow for mutation after gift/sale deeds registration"
        );

        // States and transitions for workflow_id=1 are not seeded; create via admin APIs.

        updateCaseNatureWorkflowCode("MUTATION_GIFT_SALE", workflowCode);
    }

    /**
     * II. Mutation (after death of landowner) Workflow
     */
    private void initializeMutationDeathWorkflow() {
        String workflowCode = "MUTATION_DEATH";
        log.info("Initializing workflow: {}", workflowCode);

        WorkflowDefinition workflow = createOrGetWorkflow(
            workflowCode,
            "Mutation (after death of landowner)",
            "Workflow for mutation after death of landowner"
        );

        Map<String, WorkflowState> states = createStates(workflow, new String[][]{
            {"CITIZEN_APPLICATION", "Citizen Application", "1", "true", "false"},
            {"MANDOL_REPORT", "Mandol Report", "2", "false", "false"},
            {"NOTICE_PUBLISHED", "Notice Published", "3", "false", "false"},
            {"OBJECTION_PERIOD", "Objection Period", "4", "false", "false"},
            {"DECISION_PENDING", "Decision Pending", "5", "false", "false"},
            {"APPROVED", "Approved", "6", "false", "false"},
            {"LAND_RECORD_UPDATED", "Land Record Updated", "7", "false", "false"},
            {"COMPLETED", "Completed", "8", "false", "true"},
            {"REJECTED", "Rejected", "9", "false", "true"},
            {"RETURNED_FOR_CORRECTION", "Returned for Correction", "10", "false", "false"}
        });

        createTransitions(workflow, states, new String[][]{
            {"SUBMIT_APPLICATION", "Submit Application", "CITIZEN_APPLICATION", "MANDOL_REPORT", "false"},
            {"PREPARE_REPORT", "Prepare Report", "MANDOL_REPORT", "NOTICE_PUBLISHED", "false"},
            {"PUBLISH_NOTICE", "Publish Notice", "NOTICE_PUBLISHED", "OBJECTION_PERIOD", "false"},
            {"WAIT_FOR_OBJECTIONS", "Wait for Objections", "OBJECTION_PERIOD", "DECISION_PENDING", "false"},
            {"APPROVE", "Approve", "DECISION_PENDING", "APPROVED", "true"},
            {"REJECT", "Reject", "DECISION_PENDING", "REJECTED", "true"},
            {"RETURN_FROM_MANDOL", "Return for Correction (Mandol)", "MANDOL_REPORT", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_SDC", "Return for Correction (SDC)", "DECISION_PENDING", "RETURNED_FOR_CORRECTION", "true"},
            {"REVIEW_CORRECTION_MANDOL", "Review Correction (Mandol)", "RETURNED_FOR_CORRECTION", "MANDOL_REPORT", "false"},
            {"UPDATE_LAND_RECORD", "Update Land Record", "APPROVED", "LAND_RECORD_UPDATED", "false"},
            {"COMPLETE", "Complete", "LAND_RECORD_UPDATED", "COMPLETED", "false"}
        });

        updateCaseNatureWorkflowCode("MUTATION_DEATH", workflowCode);
    }

    /**
     * III. Partition (division of land parcel) Workflow
     */
    private void initializePartitionWorkflow() {
        String workflowCode = "PARTITION";
        log.info("Initializing workflow: {}", workflowCode);

        WorkflowDefinition workflow = createOrGetWorkflow(
            workflowCode,
            "Partition (division of land parcel)",
            "Workflow for partition of land parcel"
        );

        Map<String, WorkflowState> states = createStates(workflow, new String[][]{
            {"CITIZEN_APPLICATION", "Citizen Application", "1", "true", "false"},
            {"DA_ENTRY", "DA Entry", "2", "false", "false"},
            {"MANDOL_RECEIVED", "Mandol Received", "3", "false", "false"},
            {"NOTICE_GENERATED", "Notice Generated", "4", "false", "false"},
            {"HEARING_SCHEDULED", "Hearing Scheduled", "5", "false", "false"},
            {"HEARING_COMPLETED", "Hearing Completed", "6", "false", "false"},
            {"SDC_DECISION_PENDING", "SDC Decision Pending", "7", "false", "false"},
            {"SDC_APPROVED", "SDC Approved", "8", "false", "false"},
            {"SDO_DECISION_PENDING", "SDO Decision Pending", "9", "false", "false"},
            {"SDO_APPROVED", "SDO Approved", "10", "false", "false"},
            {"MANDOL_UPDATE", "Mandol Update", "11", "false", "false"},
            {"LAND_RECORD_UPDATED", "Land Record Updated", "12", "false", "false"},
            {"COMPLETED", "Completed", "13", "false", "true"},
            {"REJECTED", "Rejected", "14", "false", "true"},
            {"RETURNED_FOR_CORRECTION", "Returned for Correction", "15", "false", "false"}
        });

        createTransitions(workflow, states, new String[][]{
            {"SUBMIT_APPLICATION", "Submit Application", "CITIZEN_APPLICATION", "DA_ENTRY", "false"},
            {"ENTER_IN_REGISTER", "Enter in Register", "DA_ENTRY", "MANDOL_RECEIVED", "false"},
            {"RECEIVE_BY_MANDOL", "Receive by Mandol", "MANDOL_RECEIVED", "NOTICE_GENERATED", "false"},
            {"GENERATE_NOTICE", "Generate Notice", "NOTICE_GENERATED", "HEARING_SCHEDULED", "false"},
            {"SCHEDULE_HEARING", "Schedule Hearing", "HEARING_SCHEDULED", "HEARING_COMPLETED", "false"},
            {"COMPLETE_HEARING", "Complete Hearing", "HEARING_COMPLETED", "SDC_DECISION_PENDING", "false"},
            {"SDC_APPROVE", "SDC Approve", "SDC_DECISION_PENDING", "SDC_APPROVED", "true"},
            {"SDC_REJECT", "SDC Reject", "SDC_DECISION_PENDING", "REJECTED", "true"},
            {"RETURN_FROM_DA", "Return for Correction (DA)", "DA_ENTRY", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_MANDOL", "Return for Correction (Mandol)", "MANDOL_RECEIVED", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_SDC", "Return for Correction (SDC)", "SDC_DECISION_PENDING", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_SDO", "Return for Correction (SDO)", "SDO_DECISION_PENDING", "RETURNED_FOR_CORRECTION", "true"},
            {"REVIEW_CORRECTION", "Review Correction", "RETURNED_FOR_CORRECTION", "DA_ENTRY", "false"},
            {"FORWARD_TO_SDO", "Forward to SDO", "SDC_APPROVED", "SDO_DECISION_PENDING", "false"},
            {"SDO_APPROVE", "SDO Approve", "SDO_DECISION_PENDING", "SDO_APPROVED", "true"},
            {"SDO_REJECT", "SDO Reject", "SDO_DECISION_PENDING", "REJECTED", "true"},
            {"PASS_TO_MANDOL", "Pass to Mandol", "SDO_APPROVED", "MANDOL_UPDATE", "false"},
            {"UPDATE_LAND_RECORD", "Update Land Record", "MANDOL_UPDATE", "LAND_RECORD_UPDATED", "false"},
            {"COMPLETE", "Complete", "LAND_RECORD_UPDATED", "COMPLETED", "false"}
        });

        updateCaseNatureWorkflowCode("PARTITION", workflowCode);
    }

    /**
     * IV. Change in Classification of Land (before 2014) Workflow
     */
    private void initializeClassificationBefore2014Workflow() {
        String workflowCode = "CLASSIFICATION_BEFORE_2014";
        log.info("Initializing workflow: {}", workflowCode);

        WorkflowDefinition workflow = createOrGetWorkflow(
            workflowCode,
            "Change in Classification of Land (before 2014)",
            "Workflow for change in land classification before 2014"
        );

        Map<String, WorkflowState> states = createStates(workflow, new String[][]{
            {"REVENUE_APPROVAL", "Revenue Department Approval", "1", "true", "false"},
            {"FEES_PAID", "Fees Paid", "2", "false", "false"},
            {"DC_ORDER", "DC Order", "3", "false", "false"},
            {"SDC_FORWARD", "SDC Forward", "4", "false", "false"},
            {"MANDOL_UPDATE", "Mandol Update", "5", "false", "false"},
            {"LAND_RECORD_UPDATED", "Land Record Updated", "6", "false", "false"},
            {"COMPLETED", "Completed", "7", "false", "true"},
            {"RETURNED_FOR_CORRECTION", "Returned for Correction", "8", "false", "false"}
        });

        createTransitions(workflow, states, new String[][]{
            {"REVENUE_APPROVE", "Revenue Approve", "REVENUE_APPROVAL", "FEES_PAID", "false"},
            {"PAY_FEES", "Pay Fees", "FEES_PAID", "DC_ORDER", "false"},
            {"DC_ISSUE_ORDER", "DC Issue Order", "DC_ORDER", "SDC_FORWARD", "false"},
            {"SDC_FORWARD_ORDER", "SDC Forward Order", "SDC_FORWARD", "MANDOL_UPDATE", "false"},
            {"RETURN_FROM_STATE", "Return for Correction (State)", "REVENUE_APPROVAL", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_DC", "Return for Correction (DC)", "DC_ORDER", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_MANDOL", "Return for Correction (Mandol)", "MANDOL_UPDATE", "RETURNED_FOR_CORRECTION", "true"},
            {"REVIEW_CORRECTION_STATE", "Review Correction (State)", "RETURNED_FOR_CORRECTION", "REVENUE_APPROVAL", "false"},
            {"UPDATE_LAND_RECORD", "Update Land Record", "MANDOL_UPDATE", "LAND_RECORD_UPDATED", "false"},
            {"COMPLETE", "Complete", "LAND_RECORD_UPDATED", "COMPLETED", "false"}
        });

        updateCaseNatureWorkflowCode("CLASSIFICATION_CHANGE_BEFORE_2014", workflowCode);
    }

    /**
     * V. Change in Classification of Land (after 2014) Workflow
     */
    private void initializeClassificationAfter2014Workflow() {
        String workflowCode = "CLASSIFICATION_AFTER_2014";
        log.info("Initializing workflow: {}", workflowCode);

        WorkflowDefinition workflow = createOrGetWorkflow(
            workflowCode,
            "Change in Classification of Land (after 2014)",
            "Workflow for change in land classification after 2014"
        );

        Map<String, WorkflowState> states = createStates(workflow, new String[][]{
            {"REVENUE_APPROVAL", "Revenue Department Approval", "1", "true", "false"},
            {"FEES_PAID", "Fees Paid", "2", "false", "false"},
            {"DC_ORDER", "DC Order", "3", "false", "true"},
            {"RETURNED_FOR_CORRECTION", "Returned for Correction", "4", "false", "false"}
        });

        createTransitions(workflow, states, new String[][]{
            {"REVENUE_APPROVE", "Revenue Approve", "REVENUE_APPROVAL", "FEES_PAID", "false"},
            {"PAY_FEES", "Pay Fees", "FEES_PAID", "DC_ORDER", "false"},
            {"RETURN_FROM_STATE", "Return for Correction (State)", "REVENUE_APPROVAL", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_DC", "Return for Correction (DC)", "DC_ORDER", "RETURNED_FOR_CORRECTION", "true"},
            {"REVIEW_CORRECTION_STATE", "Review Correction (State)", "RETURNED_FOR_CORRECTION", "REVENUE_APPROVAL", "false"}
        });

        updateCaseNatureWorkflowCode("CLASSIFICATION_CHANGE_AFTER_2014", workflowCode);
    }

    /**
     * VI. Implementation of order passed by a Higher Court Workflow
     */
    private void initializeCourtOrderWorkflow() {
        String workflowCode = "COURT_ORDER";
        log.info("Initializing workflow: {}", workflowCode);

        WorkflowDefinition workflow = createOrGetWorkflow(
            workflowCode,
            "Implementation of order passed by a Higher Court",
            "Workflow for implementing higher court orders"
        );

        Map<String, WorkflowState> states = createStates(workflow, new String[][]{
            {"COURT_ORDER_RECEIVED", "Court Order Received", "1", "true", "false"},
            {"SDC_MUTATION_CASE", "SDC Mutation Case", "2", "false", "false"},
            {"FOLLOW_MUTATION_WORKFLOW", "Follow Mutation Workflow", "3", "false", "true"},
            {"RETURNED_FOR_CORRECTION", "Returned for Correction", "4", "false", "false"}
        });

        createTransitions(workflow, states, new String[][]{
            {"RECEIVE_COURT_ORDER", "Receive Court Order", "COURT_ORDER_RECEIVED", "SDC_MUTATION_CASE", "false"},
            {"CREATE_MUTATION_CASE", "Create Mutation Case", "SDC_MUTATION_CASE", "FOLLOW_MUTATION_WORKFLOW", "false"},
            {"RETURN_FROM_SDC", "Return for Correction (SDC)", "SDC_MUTATION_CASE", "RETURNED_FOR_CORRECTION", "true"},
            {"REVIEW_CORRECTION_SDC", "Review Correction (SDC)", "RETURNED_FOR_CORRECTION", "SDC_MUTATION_CASE", "false"}
        });

        updateCaseNatureWorkflowCode("HIGHER_COURT_ORDER", workflowCode);
    }

    /**
     * VII. Allotment of Land Workflow
     */
    private void initializeAllotmentWorkflow() {
        String workflowCode = "ALLOTMENT";
        log.info("Initializing workflow: {}", workflowCode);

        WorkflowDefinition workflow = createOrGetWorkflow(
            workflowCode,
            "Allotment of Land",
            "Workflow for land allotment"
        );

        Map<String, WorkflowState> states = createStates(workflow, new String[][]{
            {"GOVERNMENT_APPROVAL", "Government Approval", "1", "true", "false"},
            {"PREMIUM_PAID", "Premium Paid", "2", "false", "false"},
            {"DEED_SIGNED", "Deed Signed", "3", "false", "false"},
            {"REGISTERED", "Registered", "4", "false", "false"},
            {"APPLICATION_TO_DC", "Application to DC", "5", "false", "false"},
            {"MANDOL_ENTRY", "Mandol Entry", "6", "false", "false"},
            {"SDC_APPROVAL", "SDC Approval", "7", "false", "false"},
            {"DC_APPROVAL", "DC Approval", "8", "false", "false"},
            {"LAND_RECORD_UPDATED", "Land Record Updated", "9", "false", "false"},
            {"COMPLETED", "Completed", "10", "false", "true"},
            {"RETURNED_FOR_CORRECTION", "Returned for Correction", "11", "false", "false"}
        });

        createTransitions(workflow, states, new String[][]{
            {"GOVERNMENT_APPROVE", "Government Approve", "GOVERNMENT_APPROVAL", "PREMIUM_PAID", "false"},
            {"PAY_PREMIUM", "Pay Premium", "PREMIUM_PAID", "DEED_SIGNED", "false"},
            {"SIGN_DEED", "Sign Deed", "DEED_SIGNED", "REGISTERED", "false"},
            {"REGISTER_DEED", "Register Deed", "REGISTERED", "APPLICATION_TO_DC", "false"},
            {"APPLY_TO_DC", "Apply to DC", "APPLICATION_TO_DC", "MANDOL_ENTRY", "false"},
            {"MANDOL_MAKE_ENTRY", "Mandol Make Entry", "MANDOL_ENTRY", "SDC_APPROVAL", "false"},
            {"SDC_APPROVE", "SDC Approve", "SDC_APPROVAL", "DC_APPROVAL", "false"},
            {"DC_APPROVE", "DC Approve", "DC_APPROVAL", "LAND_RECORD_UPDATED", "true"},
            {"RETURN_FROM_STATE", "Return for Correction (State)", "GOVERNMENT_APPROVAL", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_MANDOL", "Return for Correction (Mandol)", "MANDOL_ENTRY", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_SDC", "Return for Correction (SDC)", "SDC_APPROVAL", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_DC", "Return for Correction (DC)", "DC_APPROVAL", "RETURNED_FOR_CORRECTION", "true"},
            {"REVIEW_CORRECTION_STATE", "Review Correction (State)", "RETURNED_FOR_CORRECTION", "GOVERNMENT_APPROVAL", "false"},
            {"UPDATE_LAND_RECORD", "Update Land Record", "LAND_RECORD_UPDATED", "COMPLETED", "false"}
        });

        updateCaseNatureWorkflowCode("ALLOTMENT", workflowCode);
    }

    /**
     * VIII. Land Acquisition (under RFCTLARR Act, 2013 or National Highways Act, 1956) Workflow
     */
    private void initializeAcquisitionRFCTLARRWorkflow() {
        String workflowCode = "ACQUISITION_RFCTLARR";
        log.info("Initializing workflow: {}", workflowCode);

        WorkflowDefinition workflow = createOrGetWorkflow(
            workflowCode,
            "Land Acquisition (under RFCTLARR Act, 2013 or National Highways Act, 1956)",
            "Workflow for land acquisition under RFCTLARR or National Highways Act"
        );

        Map<String, WorkflowState> states = createStates(workflow, new String[][]{
            {"DC_COMPENSATION_ORDER", "DC Compensation Order", "1", "true", "false"},
            {"DC_HANDOVER_ORDER", "DC Handover Order", "2", "false", "false"},
            {"SDC_MUTATION_ORDER", "SDC Mutation Order", "3", "false", "false"},
            {"MUTATION_UPDATED", "Mutation Updated", "4", "false", "false"},
            {"SDO_PARTITION_ORDER", "SDO Partition Order", "5", "false", "false"},
            {"PARTITION_UPDATED", "Partition Updated", "6", "false", "false"},
            {"COMPLETED", "Completed", "7", "false", "true"},
            {"RETURNED_FOR_CORRECTION", "Returned for Correction", "8", "false", "false"}
        });

        createTransitions(workflow, states, new String[][]{
            {"DC_COMPENSATION_AWARD", "DC Compensation Award", "DC_COMPENSATION_ORDER", "DC_HANDOVER_ORDER", "false"},
            {"DC_HANDOVER", "DC Handover", "DC_HANDOVER_ORDER", "SDC_MUTATION_ORDER", "false"},
            {"SDC_MUTATION", "SDC Mutation", "SDC_MUTATION_ORDER", "MUTATION_UPDATED", "false"},
            {"UPDATE_MUTATION", "Update Mutation", "MUTATION_UPDATED", "SDO_PARTITION_ORDER", "false"},
            {"SDO_PARTITION", "SDO Partition", "SDO_PARTITION_ORDER", "PARTITION_UPDATED", "false"},
            {"UPDATE_PARTITION", "Update Partition", "PARTITION_UPDATED", "COMPLETED", "false"},
            {"RETURN_FROM_DC", "Return for Correction (DC)", "DC_COMPENSATION_ORDER", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_SDC", "Return for Correction (SDC)", "SDC_MUTATION_ORDER", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_SDO", "Return for Correction (SDO)", "SDO_PARTITION_ORDER", "RETURNED_FOR_CORRECTION", "true"},
            {"REVIEW_CORRECTION_DC", "Review Correction (DC)", "RETURNED_FOR_CORRECTION", "DC_COMPENSATION_ORDER", "false"}
        });

        updateCaseNatureWorkflowCode("LAND_ACQUISITION_RFCTLARR_NHA", workflowCode);
    }

    /**
     * IX. Land Acquisition (under Direct Purchase) Workflow
     */
    private void initializeAcquisitionDirectPurchaseWorkflow() {
        String workflowCode = "ACQUISITION_DIRECT_PURCHASE";
        log.info("Initializing workflow: {}", workflowCode);

        WorkflowDefinition workflow = createOrGetWorkflow(
            workflowCode,
            "Land Acquisition (under Direct Purchase)",
            "Workflow for land acquisition under direct purchase"
        );

        Map<String, WorkflowState> states = createStates(workflow, new String[][]{
            {"GOVERNMENT_APPROVAL", "Government Approval", "1", "true", "false"},
            {"SALE_DEED_REGISTERED", "Sale Deed Registered", "2", "false", "false"},
            {"SDC_MUTATION_ORDER", "SDC Mutation Order", "3", "false", "false"},
            {"SDO_PARTITION_ORDER", "SDO Partition Order", "4", "false", "false"},
            {"NEW_PATTA_COMPUTERIZATION", "New Patta Computerization", "5", "false", "false"},
            {"MANDOL_REPORT", "Mandol Report", "6", "false", "false"},
            {"DC_APPROVAL", "DC Approval", "7", "false", "false"},
            {"LAND_RECORD_UPDATED", "Land Record Updated", "8", "false", "false"},
            {"COMPLETED", "Completed", "9", "false", "true"},
            {"RETURNED_FOR_CORRECTION", "Returned for Correction", "10", "false", "false"}
        });

        createTransitions(workflow, states, new String[][]{
            {"GOVERNMENT_APPROVE", "Government Approve", "GOVERNMENT_APPROVAL", "SALE_DEED_REGISTERED", "false"},
            {"REGISTER_SALE_DEED", "Register Sale Deed", "SALE_DEED_REGISTERED", "SDC_MUTATION_ORDER", "false"},
            {"SDC_MUTATION", "SDC Mutation", "SDC_MUTATION_ORDER", "SDO_PARTITION_ORDER", "false"},
            {"SDO_PARTITION", "SDO Partition", "SDO_PARTITION_ORDER", "NEW_PATTA_COMPUTERIZATION", "false"},
            {"APPLY_COMPUTERIZATION", "Apply Computerization", "NEW_PATTA_COMPUTERIZATION", "MANDOL_REPORT", "false"},
            {"MANDOL_PREPARE_REPORT", "Mandol Prepare Report", "MANDOL_REPORT", "DC_APPROVAL", "false"},
            {"DC_APPROVE", "DC Approve", "DC_APPROVAL", "LAND_RECORD_UPDATED", "true"},
            {"RETURN_FROM_STATE", "Return for Correction (State)", "GOVERNMENT_APPROVAL", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_SDC", "Return for Correction (SDC)", "SDC_MUTATION_ORDER", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_SDO", "Return for Correction (SDO)", "SDO_PARTITION_ORDER", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_MANDOL", "Return for Correction (Mandol)", "MANDOL_REPORT", "RETURNED_FOR_CORRECTION", "true"},
            {"RETURN_FROM_DC", "Return for Correction (DC)", "DC_APPROVAL", "RETURNED_FOR_CORRECTION", "true"},
            {"REVIEW_CORRECTION_STATE", "Review Correction (State)", "RETURNED_FOR_CORRECTION", "GOVERNMENT_APPROVAL", "false"},
            {"UPDATE_LAND_RECORD", "Update Land Record", "LAND_RECORD_UPDATED", "COMPLETED", "false"}
        });

        updateCaseNatureWorkflowCode("ACQUISITION_DIRECT_PURCHASE", workflowCode);
    }

    /**
     * Helper method to create or get workflow definition
     */
    private WorkflowDefinition createOrGetWorkflow(String code, String name, String description) {
        return workflowDefinitionRepository.findByWorkflowCode(code)
            .orElseGet(() -> {
                WorkflowDefinition workflow = new WorkflowDefinition();
                workflow.setWorkflowCode(code);
                workflow.setWorkflowName(name);
                workflow.setDescription(description);
                workflow.setIsActive(true);
                workflow.setVersion(1);
                workflow.setCreatedAt(LocalDateTime.now());
                workflow.setUpdatedAt(LocalDateTime.now());
                WorkflowDefinition saved = workflowDefinitionRepository.save(workflow);
                log.info("Created workflow: {} - {}", code, name);
                return saved;
            });
    }

    /**
     * Helper method to create states for a workflow
     */
    private Map<String, WorkflowState> createStates(WorkflowDefinition workflow, String[][] stateData) {
        Map<String, WorkflowState> states = new HashMap<>();
        
        for (String[] data : stateData) {
            String stateCode = data[0];
            String stateName = data[1];
            int stateOrder = Integer.parseInt(data[2]);
            boolean isInitial = Boolean.parseBoolean(data[3]);
            boolean isFinal = Boolean.parseBoolean(data[4]);

            WorkflowState state = workflowStateRepository
                .findByWorkflowIdAndStateCode(workflow.getId(), stateCode)
                .orElseGet(() -> {
                    WorkflowState newState = new WorkflowState();
                    newState.setWorkflow(workflow);
                    newState.setWorkflowId(workflow.getId());
                    newState.setStateCode(stateCode);
                    newState.setStateName(stateName);
                    newState.setStateOrder(stateOrder);
                    newState.setIsInitialState(isInitial);
                    newState.setIsFinalState(isFinal);
                    newState.setDescription(stateName);
                    newState.setCreatedAt(LocalDateTime.now());
                    WorkflowState saved = workflowStateRepository.save(newState);
                    log.debug("Created state: {} - {}", stateCode, stateName);
                    return saved;
                });
            
            states.put(stateCode, state);
        }
        
        return states;
    }

    /**
     * Helper method to create transitions for a workflow
     */
    private void createTransitions(WorkflowDefinition workflow, Map<String, WorkflowState> states, 
                                   String[][] transitionData) {
        for (String[] data : transitionData) {
            String transitionCode = data[0];
            String transitionName = data[1];
            String fromStateCode = data[2];
            String toStateCode = data[3];
            boolean requiresComment = Boolean.parseBoolean(data[4]);

            WorkflowState fromState = states.get(fromStateCode);
            WorkflowState toState = states.get(toStateCode);

            if (fromState == null || toState == null) {
                log.warn("State not found for transition: {} (from: {}, to: {})", 
                    transitionCode, fromStateCode, toStateCode);
                continue;
            }

            transitionRepository.findByWorkflowIdAndTransitionCode(workflow.getId(), transitionCode)
                .orElseGet(() -> {
                    WorkflowTransition transition = new WorkflowTransition();
                    transition.setWorkflow(workflow);
                    transition.setWorkflowId(workflow.getId());
                    transition.setFromState(fromState);
                    transition.setFromStateId(fromState.getId());
                    transition.setToState(toState);
                    transition.setToStateId(toState.getId());
                    transition.setTransitionCode(transitionCode);
                    transition.setTransitionName(transitionName);
                    transition.setIsActive(true);
                    transition.setRequiresComment(requiresComment);
                    transition.setDescription(transitionName);
                    transition.setCreatedAt(LocalDateTime.now());
                    WorkflowTransition saved = transitionRepository.save(transition);
                    log.debug("Created transition: {} - {}", transitionCode, transitionName);
                    
                    // Create permissions for this transition
                    createPermissionsForTransition(saved, fromStateCode, toStateCode);
                    
                    return saved;
                });
        }
    }

    /**
     * Helper method to create permissions for a transition based on workflow logic
     */
    private void createPermissionsForTransition(WorkflowTransition transition, String fromStateCode, String toStateCode) {
        String transitionCode = transition.getTransitionCode();
        Long transitionId = transition.getId();

        // Default permissions based on transition code patterns
        if (transitionCode.contains("SUBMIT") || transitionCode.contains("APPLICATION")) {
            // Citizen can submit applications
            createPermissionIfNotExists(transitionId, "CITIZEN", null, true, false, "ANY_UNIT");
            // READER can accept/register cases (moves from CITIZEN_APPLICATION to DA_ENTRY)
            createPermissionIfNotExists(transitionId, "READER", AdminUnit.UnitLevel.DISTRICT, true, false, "SAME_UNIT");
        }
        
        if (transitionCode.contains("ENTER") || transitionCode.contains("REGISTER")) {
            // DA can enter in register
            createPermissionIfNotExists(transitionId, "DEALING_ASSISTANT", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
        }
        
        if (transitionCode.contains("MANDOL") || transitionCode.contains("RECEIVE") || transitionCode.contains("PREPARE")) {
            // Circle Mandol can receive and prepare
            createPermissionIfNotExists(transitionId, "CIRCLE_MANDOL", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
        }
        
        if (transitionCode.contains("NOTICE") || transitionCode.contains("GENERATE") || transitionCode.contains("PUBLISH")) {
            // Circle Mandol or SDC can generate notices
            createPermissionIfNotExists(transitionId, "CIRCLE_MANDOL", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
            createPermissionIfNotExists(transitionId, "CIRCLE_OFFICER", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
        }
        
        if (transitionCode.contains("HEARING") || transitionCode.contains("SCHEDULE")) {
            // SDC can schedule hearings
            createPermissionIfNotExists(transitionId, "CIRCLE_OFFICER", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
        }
        
        if (transitionCode.contains("APPROVE") || transitionCode.contains("DECISION")) {
            // SDC, SDO, or DC can approve based on level
            if (transitionCode.contains("SDC")) {
                createPermissionIfNotExists(transitionId, "CIRCLE_OFFICER", AdminUnit.UnitLevel.CIRCLE, true, true, "SAME_UNIT");
            } else if (transitionCode.contains("SDO")) {
                createPermissionIfNotExists(transitionId, "SUB_DIVISION_OFFICER", AdminUnit.UnitLevel.SUB_DIVISION, true, true, "PARENT_UNIT");
            } else if (transitionCode.contains("DC")) {
                createPermissionIfNotExists(transitionId, "DISTRICT_OFFICER", AdminUnit.UnitLevel.DISTRICT, true, true, "PARENT_UNIT");
            } else {
                // Default approval - SDC level
                createPermissionIfNotExists(transitionId, "CIRCLE_OFFICER", AdminUnit.UnitLevel.CIRCLE, true, true, "SAME_UNIT");
            }
        }
        
        if (transitionCode.contains("REJECT")) {
            // Same as approve for rejection
            if (transitionCode.contains("SDC")) {
                createPermissionIfNotExists(transitionId, "CIRCLE_OFFICER", AdminUnit.UnitLevel.CIRCLE, true, true, "SAME_UNIT");
            } else if (transitionCode.contains("SDO")) {
                createPermissionIfNotExists(transitionId, "SUB_DIVISION_OFFICER", AdminUnit.UnitLevel.SUB_DIVISION, true, true, "PARENT_UNIT");
            } else {
                createPermissionIfNotExists(transitionId, "CIRCLE_OFFICER", AdminUnit.UnitLevel.CIRCLE, true, true, "SAME_UNIT");
            }
        }

        if (transitionCode.contains("RETURN_FROM_DA")) {
            createPermissionIfNotExists(transitionId, "DEALING_ASSISTANT", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
        }

        if (transitionCode.contains("RETURN_FROM_MANDOL")) {
            createPermissionIfNotExists(transitionId, "CIRCLE_MANDOL", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
        }

        if (transitionCode.contains("RETURN_FROM_SDC")) {
            createPermissionIfNotExists(transitionId, "CIRCLE_OFFICER", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
        }

        if (transitionCode.contains("RETURN_FROM_SDO")) {
            createPermissionIfNotExists(transitionId, "SUB_DIVISION_OFFICER", AdminUnit.UnitLevel.SUB_DIVISION, true, false, "SAME_UNIT");
        }

        if (transitionCode.contains("RETURN_FROM_DC")) {
            createPermissionIfNotExists(transitionId, "DISTRICT_OFFICER", AdminUnit.UnitLevel.DISTRICT, true, false, "SAME_UNIT");
        }

        if (transitionCode.contains("RETURN_FROM_STATE")) {
            createPermissionIfNotExists(transitionId, "STATE_ADMIN", AdminUnit.UnitLevel.STATE, true, false, "SAME_UNIT");
        }

        if ("REVIEW_CORRECTION".equals(transitionCode)) {
            createPermissionIfNotExists(transitionId, "DEALING_ASSISTANT", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
        }

        if (transitionCode.contains("REVIEW_CORRECTION_MANDOL")) {
            createPermissionIfNotExists(transitionId, "CIRCLE_MANDOL", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
        }

        if (transitionCode.contains("REVIEW_CORRECTION_SDC")) {
            createPermissionIfNotExists(transitionId, "CIRCLE_OFFICER", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
        }

        if (transitionCode.contains("REVIEW_CORRECTION_SDO")) {
            createPermissionIfNotExists(transitionId, "SUB_DIVISION_OFFICER", AdminUnit.UnitLevel.SUB_DIVISION, true, false, "SAME_UNIT");
        }

        if (transitionCode.contains("REVIEW_CORRECTION_DC")) {
            createPermissionIfNotExists(transitionId, "DISTRICT_OFFICER", AdminUnit.UnitLevel.DISTRICT, true, false, "SAME_UNIT");
        }

        if (transitionCode.contains("REVIEW_CORRECTION_STATE")) {
            createPermissionIfNotExists(transitionId, "STATE_ADMIN", AdminUnit.UnitLevel.STATE, true, false, "SAME_UNIT");
        }
        
        if (transitionCode.contains("UPDATE") || transitionCode.contains("LAND_RECORD")) {
            // Circle Mandol or DA can update land records
            createPermissionIfNotExists(transitionId, "CIRCLE_MANDOL", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
            createPermissionIfNotExists(transitionId, "DEALING_ASSISTANT", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
        }
        
        if (transitionCode.contains("FORWARD") || transitionCode.contains("PASS")) {
            // SDC can forward to SDO, SDO can forward to DC
            if (transitionCode.contains("SDO")) {
                createPermissionIfNotExists(transitionId, "CIRCLE_OFFICER", AdminUnit.UnitLevel.CIRCLE, true, false, "PARENT_UNIT");
            } else if (transitionCode.contains("DC")) {
                createPermissionIfNotExists(transitionId, "SUB_DIVISION_OFFICER", AdminUnit.UnitLevel.SUB_DIVISION, true, false, "PARENT_UNIT");
            } else {
                createPermissionIfNotExists(transitionId, "CIRCLE_OFFICER", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
            }
        }
        
        if (transitionCode.contains("COMPLETE") || transitionCode.contains("PATTA")) {
            // DA can complete and prepare patta
            createPermissionIfNotExists(transitionId, "DEALING_ASSISTANT", AdminUnit.UnitLevel.CIRCLE, true, false, "SAME_UNIT");
        }
    }

    /**
     * Helper method to create permission if it doesn't exist
     */
    private void createPermissionIfNotExists(Long transitionId, String roleCode, AdminUnit.UnitLevel unitLevel,
                                           boolean canInitiate, boolean canApprove, String hierarchyRule) {
        boolean exists = permissionRepository.existsPermissionForTransitionAndRole(
            transitionId, roleCode, unitLevel);
        
        if (!exists) {
            WorkflowTransition transition = transitionRepository.findById(transitionId).orElse(null);
            RoleMaster role = roleMasterRepository.findByRoleCode(roleCode).orElse(null);
            if (transition == null) {
                log.warn("Transition not found for id: {}, skipping permission create", transitionId);
                return;
            }
            WorkflowPermission permission = new WorkflowPermission();
            permission.setTransition(transition);
            permission.setRoleCode(roleCode);
            if (role != null) permission.setRole(role);
            permission.setUnitLevel(unitLevel);
            permission.setCanInitiate(canInitiate);
            permission.setCanApprove(canApprove);
            permission.setHierarchyRule(hierarchyRule);
            permission.setIsActive(true);
            permission.setCreatedAt(LocalDateTime.now());
            permissionRepository.save(permission);
            log.debug("Created permission: transitionId={}, roleCode={}, unitLevel={}", 
                transitionId, roleCode, unitLevel);
        }
    }

    /**
     * Helper method to update CaseNature with workflow code
     */
    private void updateCaseNatureWorkflowCode(String caseNatureCode, String workflowCode) {
        caseNatureRepository.findByCode(caseNatureCode).ifPresent(caseNature -> {
            if (caseNature.getWorkflowCode() == null || !caseNature.getWorkflowCode().equals(workflowCode)) {
                caseNature.setWorkflowCode(workflowCode);
                caseNatureRepository.save(caseNature);
                log.info("Updated CaseNature {} with workflow code: {}", caseNatureCode, workflowCode);
            }
        });
    }
}

