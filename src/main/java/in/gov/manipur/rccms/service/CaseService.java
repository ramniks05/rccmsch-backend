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
import java.util.List;
import java.util.Map;
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
    private final CaseTypeRepository caseTypeRepository;
    private final CitizenRepository citizenRepository;
    private final AdminUnitRepository adminUnitRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final CaseWorkflowInstanceRepository workflowInstanceRepository;
    private final FormSchemaService formSchemaService;
    private final ObjectMapper objectMapper;

    /**
     * Create a new case
     */
    public CaseDTO createCase(CreateCaseDTO dto, Long applicantId) {
        log.info("Creating new case: caseTypeId={}, applicantId={}, unitId={}", 
                dto.getCaseTypeId(), applicantId, dto.getUnitId());

        // Validate case type
        Long caseTypeId = dto.getCaseTypeId();
        if (caseTypeId == null) {
            throw new IllegalArgumentException("Case type ID cannot be null");
        }
        CaseType caseType = caseTypeRepository.findById(caseTypeId)
                .orElseThrow(() -> new RuntimeException("Case type not found: " + caseTypeId));

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

        // Validate form data against schema if caseData is provided
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

        // Generate case number
        String caseNumber = generateCaseNumber(caseType, unit);

        // Create case entity
        Case caseEntity = new Case();
        caseEntity.setCaseNumber(caseNumber);
        caseEntity.setCaseType(caseType);
        Long caseTypeIdValue = caseType.getId();
        if (caseTypeIdValue != null) {
            caseEntity.setCaseTypeId(caseTypeIdValue);
        }
        caseEntity.setApplicant(applicant);
        caseEntity.setApplicantId(applicantIdValue);
        caseEntity.setUnit(unit);
        caseEntity.setUnitId(unitId);
        caseEntity.setSubject(dto.getSubject());
        caseEntity.setDescription(dto.getDescription());
        caseEntity.setPriority(dto.getPriority() != null ? dto.getPriority() : "MEDIUM");
        caseEntity.setApplicationDate(dto.getApplicationDate() != null ? dto.getApplicationDate() : LocalDate.now());
        caseEntity.setRemarks(dto.getRemarks());
        caseEntity.setCaseData(dto.getCaseData());
        caseEntity.setIsActive(true);

        Case savedCase = caseRepository.save(caseEntity);

        // Initialize workflow instance
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
                Map<String, String> validationErrors = formSchemaService.validateFormData(caseEntity.getCaseTypeId(), formData);

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

        log.info("Case resubmitted successfully: caseId={}", caseId);
        return convertToDTO(saved);
    }

    /**
     * Initialize workflow instance for a case
     */
    private void initializeWorkflowInstance(Case caseEntity, CaseType caseType) {
        // Get workflow code from case type
        String workflowCode = caseType.getWorkflowCode();
        if (workflowCode == null || workflowCode.isEmpty()) {
            log.warn("No workflow code configured for case type: {}. Skipping workflow initialization.", 
                    caseType.getCode());
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
        // Assignment to officer can be done later based on workflow logic

        workflowInstanceRepository.save(instance);

        // Update case status
        caseEntity.setStatus(initialState.getStateCode());
        caseRepository.save(caseEntity);

        log.info("Workflow instance initialized for case: {}, workflow: {}, initial state: {}", 
                caseEntity.getCaseNumber(), workflowCode, initialState.getStateCode());
    }

    /**
     * Generate unique case number
     * Format: CASE_TYPE_CODE-UNIT_CODE-YYYYMMDD-XXXX
     */
    private String generateCaseNumber(CaseType caseType, AdminUnit unit) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = caseType.getCode() + "-" + unit.getUnitCode() + "-" + dateStr + "-";
        
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
     * Convert Entity to DTO
     */
    private CaseDTO convertToDTO(Case caseEntity) {
        CaseDTO dto = new CaseDTO();
        dto.setId(caseEntity.getId());
        dto.setCaseNumber(caseEntity.getCaseNumber());
        dto.setCaseTypeId(caseEntity.getCaseTypeId());
        if (caseEntity.getCaseType() != null) {
            dto.setCaseTypeName(caseEntity.getCaseType().getName());
            dto.setCaseTypeCode(caseEntity.getCaseType().getCode());
        }
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
}

