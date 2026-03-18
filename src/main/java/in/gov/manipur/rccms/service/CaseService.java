package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.CaseDTO;
import in.gov.manipur.rccms.dto.CasePartiesDTO;
import in.gov.manipur.rccms.dto.CreateCaseDTO;
import in.gov.manipur.rccms.dto.FormDataDisplayItemDTO;
import in.gov.manipur.rccms.dto.FormFieldDefinitionDTO;
import in.gov.manipur.rccms.dto.FormSchemaDTO;
import in.gov.manipur.rccms.dto.PartyInfoDTO;
import in.gov.manipur.rccms.dto.ResubmitCaseDTO;
import in.gov.manipur.rccms.entity.*;
import in.gov.manipur.rccms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Case Service
 * Handles case creation, retrieval, and management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseTypeRepository caseTypeRepository; // For CaseType (NEW_FILE, APPEAL, etc.)
    private final CaseNatureRepository caseNatureRepository; // For CaseNature (MUTATION_GIFT_SALE, etc.)
    private final CitizenRepository citizenRepository;
    private final AdminUnitRepository adminUnitRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final WorkflowPermissionRepository permissionRepository;
    private final CaseWorkflowInstanceRepository workflowInstanceRepository;
    private final FormSchemaService formSchemaService;
    private final ObjectMapper objectMapper;
    private final CourtRepository courtRepository;
    private final OfficerDaHistoryRepository postingRepository;
    private final OfficerRepository officerRepository;
    private final RoleMasterRepository roleMasterRepository;
    private final WorkflowEngineService workflowEngineService;
    private final CaseModuleFormSubmissionRepository moduleFormSubmissionRepository;

    /**
     * Create a new case
     */
    public CaseDTO createCase(CreateCaseDTO dto, Long applicantId) {
        log.info("Creating new case: caseTypeId={}, caseNatureId={}, applicantId={}, unitId={}", 
                dto.getCaseTypeId(), dto.getCaseNatureId(), applicantId, dto.getUnitId());

        // Validate case type (NEW_FILE, APPEAL, etc.)
        Long caseTypeId = dto.getCaseTypeId();
        if (caseTypeId == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }
        CaseType caseType = caseTypeRepository.findById(caseTypeId)
                .orElseThrow(() -> new RuntimeException("Case type not found: " + caseTypeId));

        // Validate case nature (MUTATION_GIFT_SALE, PARTITION, etc.)
        Long caseNatureId = dto.getCaseNatureId();
        if (caseNatureId == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        CaseNature caseNature = caseNatureRepository.findById(caseNatureId)
                .orElseThrow(() -> new RuntimeException("Case nature not found: " + caseNatureId));

        // Validate applicant
        Long applicantIdValue = applicantId;
        if (applicantIdValue == null) {
            throw new IllegalArgumentException("Applicant ID cannot be null");
        }
        Citizen applicant = citizenRepository.findById(applicantIdValue)
                .orElseThrow(() -> new RuntimeException("Applicant not found: " + applicantIdValue));

        // Validate unit
        Long unitId = dto.getUnitId();
        if (unitId == null) {
            throw new IllegalArgumentException("Unit ID cannot be null");
        }
        AdminUnit unit = adminUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Unit not found: " + unitId));

        // Validate form data against schema if caseData is provided (use caseTypeId for form schema)
        if (dto.getCaseData() != null && !dto.getCaseData().trim().isEmpty()) {
            try {
                Map<String, Object> formData = objectMapper.readValue(dto.getCaseData(), 
                        new TypeReference<Map<String, Object>>() {});
                Map<String, String> validationErrors = formSchemaService.validateFormData(caseTypeId, formData);
                
                if (!validationErrors.isEmpty()) {
                    StringBuilder errorMsg = new StringBuilder("Form validation failed: ");
                    validationErrors.forEach((field, error) -> 
                        errorMsg.append(field).append(" - ").append(error).append("; "));
                    throw new IllegalArgumentException(errorMsg.toString());
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid JSON format in caseData: " + e.getMessage());
            }
        }

        // Generate case number (use caseNature code)
        String caseNumber = generateCaseNumber(caseNature, unit);

        // Create case entity
        Case caseEntity = new Case();
        caseEntity.setCaseNumber(caseNumber);
        caseEntity.setCaseType(caseType); // CaseType (NEW_FILE, APPEAL, etc.)
        Long caseTypeIdValue = caseType.getId();
        if (caseTypeIdValue != null) {
            caseEntity.setCaseTypeId(caseTypeIdValue);
        }
        caseEntity.setCaseNature(caseNature); // CaseNature (MUTATION_GIFT_SALE, PARTITION, etc.)
        Long caseNatureIdValue = caseNature.getId();
        if (caseNatureIdValue != null) {
            caseEntity.setCaseNatureId(caseNatureIdValue);
        }
        caseEntity.setApplicant(applicant);
        caseEntity.setApplicantId(applicantIdValue);
        caseEntity.setUnit(unit);
        caseEntity.setUnitId(unitId);
        
        // Set court if provided
        if (dto.getCourtId() != null) {
            in.gov.manipur.rccms.entity.Court court = courtRepository.findById(dto.getCourtId())
                    .orElseThrow(() -> new RuntimeException("Court not found: " + dto.getCourtId()));
            caseEntity.setCourt(court);
            caseEntity.setCourtId(dto.getCourtId());
        }
        
        // Set original order level (for appeals)
        caseEntity.setOriginalOrderLevel(dto.getOriginalOrderLevel());
        
        caseEntity.setSubject(dto.getSubject());
        caseEntity.setDescription(dto.getDescription());
        caseEntity.setPriority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM");
        caseEntity.setApplicationDate(dto.getApplicationDate() != null ? dto.getApplicationDate() : LocalDate.now());
        caseEntity.setRemarks(dto.getRemarks());
        caseEntity.setCaseData(dto.getCaseData());
        if (dto.getCaseData() != null && !dto.getCaseData().trim().isEmpty()) {
            try {
                Map<String, Object> formDataMap = objectMapper.readValue(dto.getCaseData(),
                        new TypeReference<Map<String, Object>>() {});
                String displayJson = buildFormDataDisplayJson(caseTypeId, formDataMap);
                caseEntity.setCaseDataDisplay(displayJson);
            } catch (Exception e) {
                log.warn("Could not build case data display for case type {}: {}", caseTypeId, e.getMessage());
            }
        }
        caseEntity.setIsActive(true);

        // Get initial workflow state and set status BEFORE saving
        // This ensures status is not null when case is first saved
        String initialStatus = getInitialWorkflowStateCode(caseType);
        caseEntity.setStatus(initialStatus);

        Case savedCase = caseRepository.save(caseEntity);

        // Initialize workflow instance (workflow code comes from CaseType)
        // This will update the status to the correct initial state if workflow exists
        initializeWorkflowInstance(savedCase, caseType);

        log.info("Case created successfully: caseNumber={}, caseId={}", caseNumber, savedCase.getId());

        return convertToDTO(savedCase);
    }

    /**
     * Resubmit a case after correction (citizen updates case data)
     */
    public CaseDTO resubmitCase(Long caseId, Long applicantId, ResubmitCaseDTO dto) {
        log.info("Resubmitting case: caseId={}, applicantId={}", caseId, applicantId);

        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        if (applicantId == null) {
            throw new IllegalArgumentException("Applicant ID cannot be null");
        }

        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        if (!applicantId.equals(caseEntity.getApplicantId())) {
            throw new RuntimeException("You are not authorized to resubmit this case");
        }

        CaseWorkflowInstance instance = workflowInstanceRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Workflow instance not found for case: " + caseId));

        WorkflowState currentState = instance.getCurrentState();
        if (currentState == null || currentState.getStateCode() == null) {
            throw new RuntimeException("Current workflow state not found for case: " + caseId);
        }

        if (!"RETURNED_FOR_CORRECTION".equals(currentState.getStateCode())) {
            throw new RuntimeException("Case is not in RETURNED_FOR_CORRECTION state");
        }

        // Validate form data against schema
        if (dto.getCaseData() != null && !dto.getCaseData().trim().isEmpty()) {
            try {
                Map<String, Object> formData = objectMapper.readValue(dto.getCaseData(),
                        new TypeReference<Map<String, Object>>() {});
                Map<String, String> validationErrors = formSchemaService.validateFormData(caseEntity.getCaseNatureId(), formData);

                if (!validationErrors.isEmpty()) {
                    StringBuilder errorMsg = new StringBuilder("Form validation failed: ");
                    validationErrors.forEach((field, error) ->
                            errorMsg.append(field).append(" - ").append(error).append("; "));
                    throw new IllegalArgumentException(errorMsg.toString());
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid JSON format in caseData: " + e.getMessage());
            }
        }

        caseEntity.setCaseData(dto.getCaseData());
        if (dto.getCaseData() != null && !dto.getCaseData().trim().isEmpty()) {
            try {
                Map<String, Object> formDataMap = objectMapper.readValue(dto.getCaseData(),
                        new TypeReference<Map<String, Object>>() {});
                String displayJson = buildFormDataDisplayJson(caseEntity.getCaseTypeId(), formDataMap);
                caseEntity.setCaseDataDisplay(displayJson);
            } catch (Exception e) {
                log.warn("Could not build case data display for resubmit case {}: {}", caseId, e.getMessage());
            }
        }
        caseEntity.setRemarks(dto.getRemarks());
        Case saved = caseRepository.save(caseEntity);

        // Move workflow back to initial state so it appears with DA again
        resetWorkflowToInitialStateAfterResubmit(saved, instance);

        log.info("Case resubmitted successfully: caseId={}", caseId);
        return convertToDTO(saved);
    }

    /**
     * Get initial workflow state code for a case type
     * Returns default status if workflow not configured
     */
    private String getInitialWorkflowStateCode(CaseType caseType) {
        String workflowCode = caseType.getWorkflowCode();
        if (workflowCode == null || workflowCode.isEmpty()) {
            log.debug("No workflow code configured for case type: {}. Using default status.", 
                    caseType.getTypeCode());
            return "PENDING"; // Default status if no workflow
        }

        try {
            // Get workflow definition
            WorkflowDefinition workflow = workflowDefinitionRepository.findByWorkflowCode(workflowCode)
                    .orElse(null);
            
            if (workflow == null) {
                log.warn("Workflow not found: {}. Using default status.", workflowCode);
                return "PENDING";
            }

            // Get initial state - configured by setting isInitialState = true on a workflow state
            // See docs/WORKFLOW_INITIAL_STATE_CONFIGURATION.md for details
            WorkflowState initialState = workflowStateRepository
                    .findByWorkflowIdAndIsInitialStateTrue(workflow.getId())
                    .orElse(null);
            
            if (initialState != null) {
                return initialState.getStateCode();
            } else {
                log.warn("Initial state not found for workflow: {}. Using default status.", workflowCode);
                return "PENDING";
            }
        } catch (Exception e) {
            log.error("Error getting initial workflow state for case type: {}. Using default status.", 
                    caseType.getTypeCode(), e);
            return "PENDING";
        }
    }

    /**
     * Initialize workflow instance for a case
     */
    private void initializeWorkflowInstance(Case caseEntity, CaseType caseType) {
        // Get workflow code from case type
        String workflowCode = caseType.getWorkflowCode();
        if (workflowCode == null || workflowCode.isEmpty()) {
            log.warn("No workflow code configured for case type: {}. Skipping workflow initialization.", 
                    caseType.getTypeCode());
            return;
        }

        // Get workflow definition
        WorkflowDefinition workflow = workflowDefinitionRepository.findByWorkflowCode(workflowCode)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowCode));

        // Get initial state - this is the state where cases start after submission
        // The initial state is configured by setting isInitialState = true on a workflow state
        // Only ONE state per workflow can have isInitialState = true
        // Configure this through Admin API: PUT /api/admin/workflow/states/{id} with isInitialState: true
        WorkflowState initialState = workflowStateRepository
                .findByWorkflowIdAndIsInitialStateTrue(workflow.getId())
                .orElseThrow(() -> new RuntimeException(
                    "Initial state not found for workflow: " + workflowCode + 
                    ". Please set isInitialState = true on one of the workflow states through Admin API."));

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
        
        roleMasterRepository.findByRoleCode("CITIZEN").ifPresent(citizenRole -> {
            instance.setAssignedToRoleId(citizenRole.getId());
            instance.setAssignedToRoleRef(citizenRole);
            instance.setAssignedToRole("CITIZEN");
        });
        if (instance.getAssignedToRole() == null) {
            instance.setAssignedToRole("CITIZEN");
        }
        instance.setAssignedToOfficer(null);
        instance.setAssignedToOfficerId(null);

        workflowInstanceRepository.save(instance);

        // Update case status
        caseEntity.setStatus(initialState.getStateCode());
        caseRepository.save(caseEntity);

        // Record "Application submitted" in history so no extra submit step is needed
        workflowEngineService.recordApplicationSubmitted(instance, initialState, caseEntity.getApplicantId());

        log.info("Workflow instance initialized for case: {}, workflow: {}, initial state: {}", 
                caseEntity.getCaseNumber(), workflowCode, initialState.getStateCode());
    }

    /**
     * Reset workflow state to initial after citizen resubmission
     */
    private void resetWorkflowToInitialStateAfterResubmit(Case caseEntity, CaseWorkflowInstance instance) {
        WorkflowDefinition workflow = instance.getWorkflow();
        if (workflow == null && instance.getWorkflowId() != null) {
            workflow = workflowDefinitionRepository.findById(instance.getWorkflowId()).orElse(null);
        }
        if (workflow == null) {
            log.warn("Workflow not found for case {}. Cannot reset to initial state.", caseEntity.getId());
            return;
        }

        WorkflowState initialState = workflowStateRepository
                .findByWorkflowIdAndIsInitialStateTrue(workflow.getId())
                .orElse(null);
        if (initialState == null) {
            log.warn("Initial state not found for workflow {}. Cannot reset case {}.", 
                    workflow.getWorkflowCode(), caseEntity.getId());
            return;
        }

        instance.setCurrentState(initialState);
        instance.setCurrentStateId(initialState.getId());

        assignCaseBasedOnWorkflowState(instance, initialState, caseEntity);
        workflowInstanceRepository.save(instance);

        caseEntity.setStatus(initialState.getStateCode());
        caseRepository.save(caseEntity);

        log.info("Case {} reset to initial state {} after resubmit", 
                caseEntity.getId(), initialState.getStateCode());
    }

    /**
     * Generate unique case number
     * Format: CASE_NATURE_CODE-UNIT_CODE-YYYYMMDD-XXXX
     */
    private String generateCaseNumber(CaseNature caseNature, AdminUnit unit) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = caseNature.getCode() + "-" + unit.getUnitCode() + "-" + dateStr + "-";
        
        // Find last case number with same prefix
        String lastCaseNumber = caseRepository.findAll().stream()
                .filter(c -> c.getCaseNumber().startsWith(prefix))
                .map(Case::getCaseNumber)
                .max(String::compareTo)
                .orElse(null);

        int sequence = 1;
        if (lastCaseNumber != null) {
            try {
                String seqStr = lastCaseNumber.substring(lastCaseNumber.lastIndexOf("-") + 1);
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (Exception e) {
                log.warn("Error parsing case number sequence: {}", lastCaseNumber);
            }
        }

        return prefix + String.format("%04d", sequence);
    }

    /**
     * Get case by ID
     */
    @Transactional(readOnly = true)
    public CaseDTO getCaseById(Long caseId) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
        return convertToDTO(caseEntity);
    }

    /**
     * Get case by case number
     */
    @Transactional(readOnly = true)
    public CaseDTO getCaseByCaseNumber(String caseNumber) {
        Case caseEntity = caseRepository.findByCaseNumber(caseNumber)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseNumber));
        return convertToDTO(caseEntity);
    }

    /**
     * Get all cases by applicant
     */
    @Transactional(readOnly = true)
    public List<CaseDTO> getCasesByApplicant(Long applicantId) {
        List<Case> cases = caseRepository.findActiveCasesByApplicant(applicantId);
        return cases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all cases by unit
     */
    @Transactional(readOnly = true)
    public List<CaseDTO> getCasesByUnit(Long unitId) {
        List<Case> cases = caseRepository.findByUnitIdOrderByApplicationDateDesc(unitId);
        return cases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get cases by status
     */
    @Transactional(readOnly = true)
    public List<CaseDTO> getCasesByStatus(String status) {
        List<Case> cases = caseRepository.findByStatusOrderByApplicationDateDesc(status);
        return cases.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get cases assigned to officer
     */
    @Transactional(readOnly = true)
    public List<CaseDTO> getCasesAssignedToOfficer(Long officerId) {
        List<CaseWorkflowInstance> instances = workflowInstanceRepository.findByAssignedToOfficerId(officerId);
        return instances.stream()
                .map(instance -> convertToDTO(instance.getCaseEntity()))
                .collect(Collectors.toList());
    }

    /**
     * Get cases for officer including unassigned cases for READER role
     * For READER: returns assigned cases + unassigned cases in their unit/court
     * For other roles: returns only assigned cases
     */
    @Transactional(readOnly = true)
    public List<CaseDTO> getCasesForOfficer(Long officerId, String roleCode, Long unitId, Long courtId) {
        log.info("getCasesForOfficer called: officerId={}, roleCode={}, unitId={}, courtId={}", 
                officerId, roleCode, unitId, courtId);
        List<CaseDTO> cases = new ArrayList<>();
        
        // Always include cases directly assigned to this officer
        List<CaseWorkflowInstance> assignedInstances = workflowInstanceRepository.findByAssignedToOfficerId(officerId);
        log.info("Found {} workflow instances assigned to officerId={}", assignedInstances.size(), officerId);
        
        if (assignedInstances.isEmpty()) {
            log.warn("No workflow instances found for officerId={}. Checking database directly...", officerId);
            // Debug: Check if there are any cases assigned to this officer ID in the database
            long count = workflowInstanceRepository.findAll().stream()
                    .filter(inst -> officerId.equals(inst.getAssignedToOfficerId()))
                    .count();
            log.warn("Total workflow instances with assignedToOfficerId={} in repository: {}", officerId, count);
        }
        
        for (CaseWorkflowInstance instance : assignedInstances) {
            log.debug("Processing workflow instance id={}, caseId={}, assignedToOfficerId={}, assignedToRole={}", 
                    instance.getId(), instance.getCaseId(), instance.getAssignedToOfficerId(), instance.getAssignedToRole());
            
            if (instance.getCaseEntity() == null) {
                log.warn("Workflow instance {} has null case entity (caseId={})", instance.getId(), instance.getCaseId());
                continue;
            }
            
            Case caseEntity = instance.getCaseEntity();
            log.debug("Case entity: id={}, caseNumber={}, isActive={}, status={}", 
                    caseEntity.getId(), caseEntity.getCaseNumber(), caseEntity.getIsActive(), caseEntity.getStatus());
            
            if (!Boolean.TRUE.equals(caseEntity.getIsActive())) {
                log.warn("Skipping inactive case {} (caseId={}) for officer {}", 
                        caseEntity.getCaseNumber(), caseEntity.getId(), officerId);
                continue;
            }
            
            cases.add(convertToDTO(caseEntity));
            log.debug("Added case {} to result list", caseEntity.getCaseNumber());
        }
        
        log.info("Returning {} active cases for officerId={}, roleCode={}", cases.size(), officerId, roleCode);
        
        // For READER role, also include unassigned cases in their unit/court
        if ("READER".equals(roleCode)) {
            List<CaseWorkflowInstance> unassignedInstances = new ArrayList<>();
            
            // Get unassigned cases by court if court-based posting
            if (courtId != null) {
                unassignedInstances = workflowInstanceRepository.findUnassignedCasesByCourt(courtId);
                log.debug("Found {} unassigned cases in court {} for READER", unassignedInstances.size(), courtId);
            } 
            // Get unassigned cases by unit if unit-based posting or if courtId is null
            else if (unitId != null) {
                unassignedInstances = workflowInstanceRepository.findUnassignedCasesByUnit(unitId);
                log.debug("Found {} unassigned cases in unit {} for READER", unassignedInstances.size(), unitId);
            }
            
            // Add unassigned cases (avoid duplicates)
            Set<Long> existingCaseIds = cases.stream()
                    .map(CaseDTO::getId)
                    .collect(Collectors.toSet());
            
            for (CaseWorkflowInstance instance : unassignedInstances) {
                if (!existingCaseIds.contains(instance.getCaseId())) {
                    cases.add(convertToDTO(instance.getCaseEntity()));
                }
            }
            
            log.info("READER {} sees {} total cases ({} assigned + {} unassigned)", 
                    officerId, cases.size(), assignedInstances.size(), 
                    cases.size() - assignedInstances.size());
        }
        
        return cases;
    }

    /**
     * Get all unassigned cases (cases not assigned to any officer)
     */
    @Transactional(readOnly = true)
    public List<CaseDTO> getUnassignedCases() {
        List<CaseWorkflowInstance> instances = workflowInstanceRepository.findUnassignedCases();
        return instances.stream()
                .map(instance -> convertToDTO(instance.getCaseEntity()))
                .collect(Collectors.toList());
    }

    /**
     * Get unassigned cases by court
     */
    @Transactional(readOnly = true)
    public List<CaseDTO> getUnassignedCasesByCourt(Long courtId) {
        List<CaseWorkflowInstance> instances = workflowInstanceRepository.findUnassignedCasesByCourt(courtId);
        return instances.stream()
                .map(instance -> convertToDTO(instance.getCaseEntity()))
                .collect(Collectors.toList());
    }

    /**
     * Get unassigned cases by unit
     */
    @Transactional(readOnly = true)
    public List<CaseDTO> getUnassignedCasesByUnit(Long unitId) {
        List<CaseWorkflowInstance> instances = workflowInstanceRepository.findUnassignedCasesByUnit(unitId);
        return instances.stream()
                .map(instance -> convertToDTO(instance.getCaseEntity()))
                .collect(Collectors.toList());
    }

    /**
     * Automatically assign a case to an officer based on court and role
     * Finds the officer posted to the case's court with the specified role
     */
    @Transactional
    public CaseDTO assignCaseToOfficer(Long caseId, String roleCode) {
        log.info("Assigning case to officer: caseId={}, roleCode={}", caseId, roleCode);

        // Get case
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        if (caseEntity.getCourtId() == null) {
            throw new RuntimeException("Case does not have a court assigned. Cannot assign to officer.");
        }

        // Get workflow instance
        CaseWorkflowInstance instance = workflowInstanceRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Workflow instance not found for case: " + caseId));

        // Check if already assigned
        if (instance.getAssignedToOfficerId() != null) {
            log.warn("Case {} is already assigned to officer {}. Current assignment will be updated.", 
                    caseId, instance.getAssignedToOfficerId());
        }

        // Find officer posted to this court with the specified role
        OfficerDaHistory posting = postingRepository
                .findByCourtIdAndRoleCodeAndIsCurrentTrue(caseEntity.getCourtId(), roleCode)
                .orElseThrow(() -> new RuntimeException(
                        "No officer found posted to court " + caseEntity.getCourtId() + 
                        " with role " + roleCode));

        instance.setAssignedToOfficer(posting.getOfficer());
        instance.setAssignedToOfficerId(posting.getOfficerId());
        instance.setAssignedToRole(roleCode);
        instance.setAssignedToRoleId(posting.getRoleId());
        if (posting.getRole() != null) instance.setAssignedToRoleRef(posting.getRole());
        workflowInstanceRepository.save(instance);
        log.info("Case {} assigned to officer {} (role: {})", caseId, posting.getOfficerId(), roleCode);

        return convertToDTO(caseEntity);
    }

    /**
     * Manually assign a case to a specific officer
     */
    @Transactional
    public CaseDTO assignCaseToSpecificOfficer(Long caseId, Long officerId, String roleCode) {
        log.info("Manually assigning case to officer: caseId={}, officerId={}, roleCode={}", 
                caseId, officerId, roleCode);

        // Validate officer exists
        Officer officer = officerRepository.findById(officerId)
                .orElseThrow(() -> new RuntimeException("Officer not found: " + officerId));

        // Validate role exists
        RoleMaster role = roleMasterRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleCode));

        // Get case
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        // Get workflow instance
        CaseWorkflowInstance instance = workflowInstanceRepository.findByCaseId(caseId)
                .orElseThrow(() -> new RuntimeException("Workflow instance not found for case: " + caseId));

        // Check if already assigned
        if (instance.getAssignedToOfficerId() != null) {
            log.warn("Case {} is already assigned to officer {}. Current assignment will be updated.", 
                    caseId, instance.getAssignedToOfficerId());
        }

        instance.setAssignedToOfficer(officer);
        instance.setAssignedToOfficerId(officerId);
        instance.setAssignedToRole(roleCode);
        instance.setAssignedToRoleId(role.getId());
        instance.setAssignedToRoleRef(role);
        workflowInstanceRepository.save(instance);
        log.info("Case {} manually assigned to officer {} (role: {})", caseId, officerId, roleCode);

        return convertToDTO(caseEntity);
    }

    /**
     * Automatically assign unassigned cases to officers based on court and default role
     * This is a batch operation to assign multiple cases
     */
    @Transactional
    public Map<String, Object> autoAssignUnassignedCases(String defaultRoleCode) {
        log.info("Auto-assigning unassigned cases with default role: {}", defaultRoleCode);

        List<CaseWorkflowInstance> unassignedInstances = workflowInstanceRepository.findUnassignedCases();
        int assignedCount = 0;
        int failedCount = 0;
        List<String> errors = new java.util.ArrayList<>();

        for (CaseWorkflowInstance instance : unassignedInstances) {
            try {
                Case caseEntity = instance.getCaseEntity();
                
                if (caseEntity.getCourtId() == null) {
                    errors.add("Case " + caseEntity.getCaseNumber() + " has no court assigned");
                    failedCount++;
                    continue;
                }

                // Try to find officer posted to this court with the default role
                Optional<OfficerDaHistory> posting = postingRepository
                        .findByCourtIdAndRoleCodeAndIsCurrentTrue(caseEntity.getCourtId(), defaultRoleCode);

                if (posting.isPresent()) {
                    OfficerDaHistory p = posting.get();
                    instance.setAssignedToOfficer(p.getOfficer());
                    instance.setAssignedToOfficerId(p.getOfficerId());
                    instance.setAssignedToRole(defaultRoleCode);
                    instance.setAssignedToRoleId(p.getRoleId());
                    if (p.getRole() != null) instance.setAssignedToRoleRef(p.getRole());
                    workflowInstanceRepository.save(instance);
                    assignedCount++;
                    log.debug("Auto-assigned case {} to officer {}", 
                            caseEntity.getCaseNumber(), posting.get().getOfficerId());
                } else {
                    errors.add("Case " + caseEntity.getCaseNumber() + 
                            ": No officer found posted to court " + caseEntity.getCourtId() + 
                            " with role " + defaultRoleCode);
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("Error auto-assigning case {}", instance.getCaseId(), e);
                errors.add("Case " + instance.getCaseId() + ": " + e.getMessage());
                failedCount++;
            }
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalUnassigned", unassignedInstances.size());
        result.put("assignedCount", assignedCount);
        result.put("failedCount", failedCount);
        result.put("errors", errors);

        log.info("Auto-assignment completed: {} assigned, {} failed out of {} total", 
                assignedCount, failedCount, unassignedInstances.size());

        return result;
    }

    /**
     * Build JSON string of form data with field labels and groups for display.
     * Uses form schema for the case type so each value is paired with fieldLabel, fieldGroup, groupLabel, displayOrder.
     */
    private String buildFormDataDisplayJson(Long caseTypeId, Map<String, Object> formData) {
        if (formData == null || formData.isEmpty()) {
            return null;
        }
        try {
            FormSchemaDTO schema = formSchemaService.getFormSchema(caseTypeId);
            if (schema == null || schema.getFields() == null || schema.getFields().isEmpty()) {
                return null;
            }
            List<FormDataDisplayItemDTO> list = new ArrayList<>();
            for (FormFieldDefinitionDTO f : schema.getFields()) {
                Object value = formData.get(f.getFieldName());
                if (value == null) {
                    value = "";
                }
                list.add(FormDataDisplayItemDTO.builder()
                        .fieldName(f.getFieldName())
                        .fieldLabel(f.getFieldLabel())
                        .fieldGroup(f.getFieldGroup())
                        .groupLabel(f.getGroupLabel())
                        .value(value)
                        .displayOrder(f.getDisplayOrder())
                        .groupDisplayOrder(f.getGroupDisplayOrder())
                        .build());
            }
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.warn("Failed to build form data display for caseTypeId {}: {}", caseTypeId, e.getMessage());
            return null;
        }
    }

    /**
     * Convert Entity to DTO
     */
    private CaseDTO convertToDTO(Case caseEntity) {
        CaseDTO dto = new CaseDTO();
        dto.setId(caseEntity.getId());
        dto.setCaseNumber(caseEntity.getCaseNumber());
        dto.setCaseTypeId(caseEntity.getCaseTypeId());
        if (caseEntity.getCaseType() != null) {
            dto.setCaseTypeName(caseEntity.getCaseType().getTypeName());
            dto.setCaseTypeCode(caseEntity.getCaseType().getTypeCode());
        }
        dto.setCaseNatureId(caseEntity.getCaseNatureId());
        if (caseEntity.getCaseNature() != null) {
            dto.setCaseNatureName(caseEntity.getCaseNature().getName());
            dto.setCaseNatureCode(caseEntity.getCaseNature().getCode());
        }
        dto.setCourtId(caseEntity.getCourtId());
        if (caseEntity.getCourt() != null) {
            dto.setCourtName(caseEntity.getCourt().getCourtName());
            dto.setCourtCode(caseEntity.getCourt().getCourtCode());
        }
        dto.setOriginalOrderLevel(caseEntity.getOriginalOrderLevel());
        dto.setApplicantId(caseEntity.getApplicantId());
        if (caseEntity.getApplicant() != null) {
            dto.setApplicantName(caseEntity.getApplicant().getFirstName() + " " + 
                    (caseEntity.getApplicant().getLastName() != null ? caseEntity.getApplicant().getLastName() : ""));
            dto.setApplicantMobile(caseEntity.getApplicant().getMobileNumber());
            dto.setApplicantEmail(caseEntity.getApplicant().getEmail());
        }
        dto.setUnitId(caseEntity.getUnitId());
        if (caseEntity.getUnit() != null) {
            dto.setUnitName(caseEntity.getUnit().getUnitName());
            dto.setUnitCode(caseEntity.getUnit().getUnitCode());
        }
        dto.setSubject(caseEntity.getSubject());
        dto.setDescription(caseEntity.getDescription());
        dto.setCaseData(caseEntity.getCaseData());
        if (caseEntity.getCaseDataDisplay() != null && !caseEntity.getCaseDataDisplay().trim().isEmpty()) {
            try {
                List<FormDataDisplayItemDTO> displayList = objectMapper.readValue(caseEntity.getCaseDataDisplay(),
                        new TypeReference<List<FormDataDisplayItemDTO>>() {});
                dto.setFormDataWithLabels(displayList);
            } catch (Exception e) {
                log.debug("Could not parse caseDataDisplay for case {}: {}", caseEntity.getId(), e.getMessage());
            }
        }
        dto.setStatus(caseEntity.getStatus());
        dto.setPriority(caseEntity.getPriority());
        dto.setApplicationDate(caseEntity.getApplicationDate());
        dto.setResolvedDate(caseEntity.getResolvedDate());
        dto.setNextHearingDate(caseEntity.getNextHearingDate());
        dto.setRemarks(caseEntity.getRemarks());
        dto.setIsActive(caseEntity.getIsActive());
        dto.setCreatedAt(caseEntity.getCreatedAt());
        dto.setUpdatedAt(caseEntity.getUpdatedAt());

        // Get workflow instance info
        Long caseEntityId = caseEntity.getId();
        if (caseEntityId != null) {
            workflowInstanceRepository.findByCaseId(caseEntityId).ifPresent(instance -> {
            dto.setWorkflowInstanceId(instance.getId());
            if (instance.getWorkflow() != null) {
                dto.setWorkflowCode(instance.getWorkflow().getWorkflowCode());
            }
            dto.setCurrentStateId(instance.getCurrentStateId());
            if (instance.getCurrentState() != null) {
                dto.setCurrentStateCode(instance.getCurrentState().getStateCode());
                dto.setCurrentStateName(instance.getCurrentState().getStateName());
                dto.setStatusName(instance.getCurrentState().getStateName());
            }
            dto.setAssignedToOfficerId(instance.getAssignedToOfficerId());
            if (instance.getAssignedToOfficer() != null) {
                dto.setAssignedToOfficerName(instance.getAssignedToOfficer().getFullName());
            }
            dto.setAssignedToRole(instance.getAssignedToRole());
            dto.setAssignedToRoleId(instance.getAssignedToRoleId());
            dto.setAssignedToUnitId(instance.getAssignedToUnitId());
            if (instance.getAssignedToUnit() != null) {
                dto.setAssignedToUnitName(instance.getAssignedToUnit().getUnitName());
            }
            // Next transition pending with which role(s) - e.g. "Pending with: Dealing Hand, Presiding Officer"
            List<String> pendingWith = workflowEngineService.getNextTransitionPendingWithRoles(caseEntityId);
            dto.setPendingWithRoleNames(pendingWith);
            dto.setPendingWithRolesDisplay(pendingWith.isEmpty() ? null : String.join(", ", pendingWith));
            });
        }

        return dto;
    }

    /**
     * Automatically assign case to officer based on workflow state and permissions
     * This method is used during case initialization
     */
    private void assignCaseBasedOnWorkflowState(CaseWorkflowInstance instance, WorkflowState state, Case caseEntity) {
        if (caseEntity.getCourtId() == null) {
            log.warn("Case {} has no court assigned. Cannot auto-assign to officer.", caseEntity.getId());
            return;
        }

        List<Long> roleIdsForState = getRoleIdsForState(instance.getWorkflowId(), state.getId());
        if (roleIdsForState.isEmpty()) {
            log.warn("No roles found with permissions for state {}. Case {} will remain unassigned.", state.getStateCode(), caseEntity.getId());
            return;
        }
        for (Long roleId : roleIdsForState) {
            Optional<OfficerDaHistory> posting = postingRepository.findByCourtIdAndRoleIdAndIsCurrentTrue(caseEntity.getCourtId(), roleId);
            if (posting.isPresent()) {
                OfficerDaHistory p = posting.get();
                instance.setAssignedToOfficer(p.getOfficer());
                instance.setAssignedToOfficerId(p.getOfficerId());
                instance.setAssignedToRole(p.getRoleCode());
                instance.setAssignedToRoleId(roleId);
                if (p.getRole() != null) instance.setAssignedToRoleRef(p.getRole());
                log.info("Case {} auto-assigned to officer {} (roleId: {}, roleCode: {}) based on initial state {}",
                        caseEntity.getId(), p.getOfficerId(), roleId, p.getRoleCode(), state.getStateCode());
                return;
            }
        }
        if (!roleIdsForState.isEmpty()) {
            Long expectedRoleId = roleIdsForState.get(0);
            instance.setAssignedToRoleId(expectedRoleId);
            roleMasterRepository.findById(expectedRoleId).ifPresent(r -> {
                instance.setAssignedToRoleRef(r);
                instance.setAssignedToRole(r.getRoleCode());
            });
            log.warn("Case {} cannot be auto-assigned. Expected roleId: {} but no officer posted to court {} with this role.",
                    caseEntity.getId(), expectedRoleId, caseEntity.getCourtId());
        }
    }

    private List<Long> getRoleIdsForState(Long workflowId, Long stateId) {
        List<Long> roleIds = new ArrayList<>();
        List<WorkflowTransition> transitions = transitionRepository.findTransitionsFromState(workflowId, stateId);
        for (WorkflowTransition transition : transitions) {
            if (!transition.getIsActive()) continue;
            for (WorkflowPermission permission : permissionRepository.findByTransitionIdAndIsActiveTrue(transition.getId())) {
                if (!permission.getCanInitiate()) continue;
                Long rid = permission.getRoleId();
                if (rid == null && permission.getRoleCode() != null) {
                    rid = roleMasterRepository.findByRoleCode(permission.getRoleCode()).map(RoleMaster::getId).orElse(null);
                }
                if (rid != null && !roleIds.contains(rid)) roleIds.add(rid);
            }
        }
        return roleIds;
    }

    /**
     * Extract parties (petitioner, respondent) from case data for attendance marking
     * Looks for common field names like: petitionerName, respondentName, petitioner_name, respondent_name, etc.
     */
    @Transactional(readOnly = true)
    public CasePartiesDTO getCaseParties(Long caseId) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        List<PartyInfoDTO> parties = new ArrayList<>();

        // Parse caseData JSON
        Map<String, Object> caseData = new java.util.HashMap<>();
        if (caseEntity.getCaseData() != null && !caseEntity.getCaseData().trim().isEmpty()) {
            try {
                caseData = objectMapper.readValue(caseEntity.getCaseData(), 
                        new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Could not parse caseData for case {}: {}", caseId, e.getMessage());
            }
        }

        // Extract petitioner (check multiple possible field names)
        String petitionerName = extractPartyName(caseData, 
                "petitionerName", "petitioner_name", "petitioner", "applicantName", "applicant_name");
        if (petitionerName != null && !petitionerName.trim().isEmpty()) {
            parties.add(PartyInfoDTO.builder()
                    .partyId("petitioner")
                    .partyName(petitionerName)
                    .partyType("PETITIONER")
                    .partyLabel("Petitioner")
                    .build());
        }

        // Extract respondent (check multiple possible field names)
        String respondentName = extractPartyName(caseData, 
                "respondentName", "respondent_name", "respondent", "oppositePartyName", "opposite_party_name");
        if (respondentName != null && !respondentName.trim().isEmpty()) {
            parties.add(PartyInfoDTO.builder()
                    .partyId("respondent")
                    .partyName(respondentName)
                    .partyType("RESPONDENT")
                    .partyLabel("Respondent")
                    .build());
        }

        // If no parties found in caseData, use applicant name as petitioner
        if (parties.isEmpty() && caseEntity.getApplicant() != null) {
            String applicantName = caseEntity.getApplicant().getFirstName() + " " + 
                    (caseEntity.getApplicant().getLastName() != null ? caseEntity.getApplicant().getLastName() : "");
            parties.add(PartyInfoDTO.builder()
                    .partyId("petitioner")
                    .partyName(applicantName.trim())
                    .partyType("PETITIONER")
                    .partyLabel("Petitioner")
                    .build());
        }

        HearingRef hearingRef = resolveLatestHearingRef(caseEntity);

        return CasePartiesDTO.builder()
                .caseId(caseId)
                .caseNumber(caseEntity.getCaseNumber())
                .parties(parties)
                .latestHearingDate(hearingRef.date())
                .latestHearingSubmissionId(hearingRef.submissionId())
                .build();
    }

    /**
     * Resolve latest hearing date for attendance flow.
     * Priority:
     * 1) case.nextHearingDate
     * 2) case.hearingDate
     * 3) latest HEARING module form submission formData date fields
     */
    private HearingRef resolveLatestHearingRef(Case caseEntity) {
        if (caseEntity == null) {
            return new HearingRef(null, null);
        }

        Optional<CaseModuleFormSubmission> latestHearingSubmission = moduleFormSubmissionRepository
                .findTopByCaseIdAndModuleTypeOrderBySubmittedAtDesc(caseEntity.getId(), ModuleType.HEARING);

        Long submissionId = latestHearingSubmission.map(CaseModuleFormSubmission::getId).orElse(null);
        LocalDate dateFromSubmission = null;
        if (latestHearingSubmission.isPresent()) {
            String formData = latestHearingSubmission.get().getFormData();
            if (formData != null && !formData.isBlank()) {
                try {
                    Map<String, Object> data = objectMapper.readValue(formData, new TypeReference<Map<String, Object>>() {});
                    String[] candidateKeys = {"nextHearingDate", "next_hearing_date", "hearingDate", "hearing_date", "date"};
                    for (String key : candidateKeys) {
                        Object value = data.get(key);
                        if (value == null) continue;
                        LocalDate parsed = parseFlexibleDate(value.toString());
                        if (parsed != null) {
                            dateFromSubmission = parsed;
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not parse HEARING formData for case {}: {}", caseEntity.getId(), e.getMessage());
                }
            }
        }

        if (caseEntity.getNextHearingDate() != null) {
            return new HearingRef(submissionId, caseEntity.getNextHearingDate());
        }
        if (caseEntity.getHearingDate() != null) {
            return new HearingRef(submissionId, caseEntity.getHearingDate());
        }

        return new HearingRef(submissionId, dateFromSubmission);
    }

    private LocalDate parseFlexibleDate(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty()) return null;

        try {
            return LocalDate.parse(value); // ISO yyyy-MM-dd
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private record HearingRef(Long submissionId, LocalDate date) {
    }

    /**
     * Extract party name from caseData map, checking multiple possible field names
     */
    private String extractPartyName(Map<String, Object> caseData, String... fieldNames) {
        for (String fieldName : fieldNames) {
            Object value = caseData.get(fieldName);
            if (value != null) {
                String name = value.toString().trim();
                if (!name.isEmpty()) {
                    return name;
                }
            }
        }
        return null;
    }
}

