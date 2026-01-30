package in.gov.manipur.rccms.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.Case;
import in.gov.manipur.rccms.entity.ModuleType;
import in.gov.manipur.rccms.repository.CaseRepository;
import in.gov.manipur.rccms.service.CaseModuleFormService;
import in.gov.manipur.rccms.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * APIs for case module forms (hearing form submission and retrieval)
 */
@Slf4j
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseModuleFormController {

    private final CaseModuleFormService moduleFormService;
    private final CaseRepository caseRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{caseId}/module-forms/{moduleType}")
    public ResponseEntity<ApiResponse<ModuleFormSchemaDTO>> getModuleFormSchema(
            @PathVariable Long caseId,
            @PathVariable ModuleType moduleType) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
        ModuleFormSchemaDTO schema = moduleFormService.getFormSchema(
                caseEntity.getCaseNatureId(), caseEntity.getCaseTypeId(), moduleType);
        return ResponseEntity.ok(ApiResponse.success("Module form schema retrieved", schema));
    }

    @GetMapping("/{caseId}/module-forms/{moduleType}/latest")
    public ResponseEntity<ApiResponse<ModuleFormSubmissionDTO>> getLatestSubmission(
            @PathVariable Long caseId,
            @PathVariable ModuleType moduleType) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        Optional<ModuleFormSubmissionDTO> submission = moduleFormService.getLatestSubmission(caseId, moduleType);
        return ResponseEntity.ok(ApiResponse.success("Latest submission retrieved", submission.orElse(null)));
    }

    @GetMapping("/{caseId}/module-forms/{moduleType}/data")
    public ResponseEntity<ApiResponse<ModuleFormWithDataDTO>> getModuleFormData(
            @PathVariable Long caseId,
            @PathVariable ModuleType moduleType) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        
        // Get case entity for schema
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
        
        // Get form schema
        ModuleFormSchemaDTO schema = moduleFormService.getFormSchema(
                caseEntity.getCaseNatureId(), caseEntity.getCaseTypeId(), moduleType);
        
        // Get latest submission data
        Optional<ModuleFormSubmissionDTO> submission = moduleFormService.getLatestSubmission(caseId, moduleType);
        
        Map<String, Object> formData = null;
        boolean hasExistingData = false;
        
        if (submission.isPresent() && submission.get().getFormData() != null) {
            try {
                formData = objectMapper.readValue(submission.get().getFormData(), 
                        new TypeReference<Map<String, Object>>() {});
                hasExistingData = true;
            } catch (Exception e) {
                log.error("Error parsing form data for case {} module {}: {}", caseId, moduleType, e.getMessage());
                // Continue with null formData if parsing fails
            }
        }
        
        ModuleFormWithDataDTO response = ModuleFormWithDataDTO.builder()
                .schema(schema)
                .formData(formData)
                .hasExistingData(hasExistingData)
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Module form schema and data retrieved", response));
    }

    @PostMapping("/{caseId}/module-forms/{moduleType}/submit")
    public ResponseEntity<ApiResponse<ModuleFormSubmissionDTO>> submitModuleForm(
            @PathVariable Long caseId,
            @PathVariable ModuleType moduleType,
            @Valid @RequestBody CreateModuleFormSubmissionDTO dto,
            HttpServletRequest request) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        Long officerId = currentUserService.getCurrentOfficerId(request);
        if (officerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Officer information not found"));
        }
        ModuleFormSubmissionDTO saved = moduleFormService.submitForm(caseId, moduleType, officerId, dto);
        return ResponseEntity.ok(ApiResponse.success("Module form submitted", saved));
    }
}

