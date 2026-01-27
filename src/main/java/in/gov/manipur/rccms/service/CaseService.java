package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.CaseDTO;
import in.gov.manipur.rccms.dto.CreateCaseDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

            // Get initial state
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

        // Get initial state
        WorkflowState initialState = workflowStateRepository
                .findByWorkflowIdAndIsInitialStateTrue(workflow.getId())
                .orElseThrow(() -> new RuntimeException("Initial state not found for workflow: " + workflowCode));

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
        
        // Automatically assign case to officer based on initial workflow state and permissions
        assignCaseBasedOnWorkflowState(instance, initialState, caseEntity);

        workflowInstanceRepository.save(instance);

        // Update case status
        caseEntity.setStatus(initialState.getStateCode());
        caseRepository.save(caseEntity);

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

        // Assign case to officer
        instance.setAssignedToOfficer(posting.getOfficer());
        instance.setAssignedToOfficerId(posting.getOfficerId());
        instance.setAssignedToRole(roleCode);
        workflowInstanceRepository.save(instance);

        log.info("Case {} assigned to officer {} (role: {})", 
                caseId, posting.getOfficerId(), roleCode);

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

        // Assign case to officer
        instance.setAssignedToOfficer(officer);
        instance.setAssignedToOfficerId(officerId);
        instance.setAssignedToRole(roleCode);
        workflowInstanceRepository.save(instance);

        log.info("Case {} manually assigned to officer {} (role: {})", 
                caseId, officerId, roleCode);

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
                    instance.setAssignedToOfficer(posting.get().getOfficer());
                    instance.setAssignedToOfficerId(posting.get().getOfficerId());
                    instance.setAssignedToRole(defaultRoleCode);
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
        dto.setStatus(caseEntity.getStatus());
        dto.setPriority(caseEntity.getPriority());
        dto.setApplicationDate(caseEntity.getApplicationDate());
        dto.setResolvedDate(caseEntity.getResolvedDate());
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
            }
            dto.setAssignedToOfficerId(instance.getAssignedToOfficerId());
            if (instance.getAssignedToOfficer() != null) {
                dto.setAssignedToOfficerName(instance.getAssignedToOfficer().getFullName());
            }
            dto.setAssignedToRole(instance.getAssignedToRole());
            dto.setAssignedToUnitId(instance.getAssignedToUnitId());
            if (instance.getAssignedToUnit() != null) {
                dto.setAssignedToUnitName(instance.getAssignedToUnit().getUnitName());
            }
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

        // Find roles that have permissions for transitions FROM this state
        List<String> rolesForState = getRolesForState(instance.getWorkflowId(), state.getId());
        
        if (rolesForState.isEmpty()) {
            log.warn("No roles found with permissions for state {}. Case {} will remain unassigned.", 
                    state.getStateCode(), caseEntity.getId());
            return;
        }

        // Try to find officer for each role (in order)
        for (String roleCode : rolesForState) {
            Optional<OfficerDaHistory> posting = postingRepository
                    .findByCourtIdAndRoleCodeAndIsCurrentTrue(caseEntity.getCourtId(), roleCode);
            
            if (posting.isPresent()) {
                // Assign case to this officer
                instance.setAssignedToOfficer(posting.get().getOfficer());
                instance.setAssignedToOfficerId(posting.get().getOfficerId());
                instance.setAssignedToRole(roleCode);
                log.info("Case {} auto-assigned to officer {} (role: {}) based on initial state {}", 
                        caseEntity.getId(), posting.get().getOfficerId(), roleCode, state.getStateCode());
                return; // Successfully assigned
            }
        }

        // No officer found - set expected role but leave unassigned
        if (!rolesForState.isEmpty()) {
            instance.setAssignedToRole(rolesForState.get(0)); // Set first expected role
            log.warn("Case {} cannot be auto-assigned. Expected role: {} but no officer posted to court {} with this role.", 
                    caseEntity.getId(), rolesForState.get(0), caseEntity.getCourtId());
        }
    }

    /**
     * Get roles that have permissions to perform transitions from a state
     */
    private List<String> getRolesForState(Long workflowId, Long stateId) {
        List<String> roles = new ArrayList<>();
        
        // Get all transitions FROM this state
        List<WorkflowTransition> transitions = transitionRepository
                .findTransitionsFromState(workflowId, stateId);
        
        // For each transition, get roles with permissions
        for (WorkflowTransition transition : transitions) {
            if (!transition.getIsActive()) {
                continue;
            }
            
            List<WorkflowPermission> permissions = permissionRepository
                    .findByTransitionIdAndIsActiveTrue(transition.getId());
            
            for (WorkflowPermission permission : permissions) {
                if (permission.getCanInitiate() && !roles.contains(permission.getRoleCode())) {
                    roles.add(permission.getRoleCode());
                }
            }
        }
        
        return roles;
    }
}

