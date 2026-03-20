package in.gov.manipur.rccms.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.Case;
import in.gov.manipur.rccms.repository.CaseRepository;
import in.gov.manipur.rccms.service.CaseModuleFormService;
import in.gov.manipur.rccms.service.CaseService;
import in.gov.manipur.rccms.service.CurrentUserService;
import in.gov.manipur.rccms.service.ModuleMasterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * APIs for case module forms (hearing form submission and retrieval)
 */
@Slf4j
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseModuleFormController {

    private final CaseModuleFormService moduleFormService;
    private final CaseService caseService;
    private final CaseRepository caseRepository;
    private final CurrentUserService currentUserService;
    private final ModuleMasterService moduleMasterService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{caseId}/module-forms/{moduleType}")
    public ResponseEntity<ApiResponse<ModuleFormSchemaDTO>> getModuleFormSchema(
            @PathVariable Long caseId,
            @PathVariable String moduleType) {
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
            @PathVariable String moduleType) {
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
            @PathVariable String moduleType) {
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

    /**
     * Get module form schema + data by numeric form id (from /permission-forms).
     * Example: GET /api/cases/{caseId}/module-forms/by-form/{formId} where formId is permission form id (e.g. 7).
     */
    @GetMapping("/{caseId}/module-forms/by-form/{formId}")
    public ResponseEntity<ApiResponse<ModuleFormWithDataDTO>> getModuleFormDataByFormId(
            @PathVariable Long caseId,
            @PathVariable Long formId) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        if (formId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Form ID cannot be null"));
        }

        ModuleFormWithDataDTO response = moduleFormService.getModuleFormDataByFormId(caseId, formId);
        return ResponseEntity.ok(ApiResponse.success("Module form schema and data retrieved by form id", response));
    }

    /**
     * Submit module form - handles both JSON and multipart/form-data
     * For FIELD_REPORT with files, use multipart/form-data
     * For other forms or FIELD_REPORT without files, use JSON
     */
    @PostMapping(value = "/{caseId}/module-forms/{moduleType}/submit",
                 consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ApiResponse<ModuleFormSubmissionDTO>> submitModuleForm(
            @PathVariable Long caseId,
            @PathVariable String moduleType,
            HttpServletRequest request) {
        log.info("Submit module form request: caseId={}, moduleType={}, contentType={}", 
                caseId, moduleType, request.getContentType());
        
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        
        if (moduleType == null) {
            log.error("Module type is null in path variable");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Module type cannot be null"));
        }
        
        Long officerId = currentUserService.getCurrentOfficerId(request);
        if (officerId == null) {
            log.warn("Officer ID not found in request for caseId={}, moduleType={}", caseId, moduleType);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Officer information not found"));
        }
        
        String contentType = request.getContentType();
        
        // Handle multipart/form-data (for FIELD_REPORT with files)
        if (contentType != null && contentType.contains("multipart/form-data")) {
            String moduleCode = moduleMasterService.normalizeModuleCode(moduleType);
            if ("SUBMIT_FIELD_REPORT".equals(moduleCode)) {
                return handleMultipartSubmission(caseId, moduleType, officerId, request);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Multipart/form-data is only supported for FIELD_REPORT module type"));
            }
        }
        
        // Handle JSON submission
        return handleJsonSubmission(caseId, moduleType, officerId, request);
    }
    
    /**
     * Handle JSON form submission
     */
    private ResponseEntity<ApiResponse<ModuleFormSubmissionDTO>> handleJsonSubmission(
            Long caseId, String moduleType, Long officerId, HttpServletRequest request) {
        try {
            CreateModuleFormSubmissionDTO dto = objectMapper.readValue(
                    request.getInputStream(), CreateModuleFormSubmissionDTO.class);
            
            log.info("Submitting form (JSON): caseId={}, moduleType={}, officerId={}", 
                    caseId, moduleType, officerId);
            
            ModuleFormSubmissionDTO saved = moduleFormService.submitForm(caseId, moduleType, officerId, dto);
            log.info("Form submitted successfully: caseId={}, moduleType={}, submissionId={}", 
                    caseId, moduleType, saved.getId());
            return ResponseEntity.ok(ApiResponse.success("Module form submitted", saved));
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument when submitting form: caseId={}, moduleType={}, error={}", 
                    caseId, moduleType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error submitting form: caseId={}, moduleType={}, error={}", 
                    caseId, moduleType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to submit form: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error submitting form: caseId={}, moduleType={}, error={}", 
                    caseId, moduleType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred: " + e.getMessage()));
        }
    }
    
    /**
     * Handle multipart/form-data submission (for FIELD_REPORT with files)
     */
    private ResponseEntity<ApiResponse<ModuleFormSubmissionDTO>> handleMultipartSubmission(
            Long caseId, String moduleType, Long officerId, HttpServletRequest request) {
        try {
            // Extract all parameters from multipart request
            Map<String, String> allParams = new HashMap<>();
            String fileMetadataJson = null;
            MultipartFile[] files = null;
            String remarks = null;
            
            // Parse multipart request manually
            if (request instanceof org.springframework.web.multipart.support.StandardMultipartHttpServletRequest) {
                org.springframework.web.multipart.support.StandardMultipartHttpServletRequest multipartRequest = 
                        (org.springframework.web.multipart.support.StandardMultipartHttpServletRequest) request;
                
                // Get all parameter names
                java.util.Enumeration<String> paramNames = multipartRequest.getParameterNames();
                while (paramNames.hasMoreElements()) {
                    String paramName = paramNames.nextElement();
                    String paramValue = multipartRequest.getParameter(paramName);
                    
                    if ("fileMetadata".equals(paramName)) {
                        fileMetadataJson = paramValue;
                    } else if ("remarks".equals(paramName)) {
                        remarks = paramValue;
                    } else if (paramName.startsWith("fileInfo_")) {
                        allParams.put(paramName, paramValue);
                    } else {
                        allParams.put(paramName, paramValue);
                    }
                }
                
                // Get files
                List<MultipartFile> fileList = new ArrayList<>();
                Iterator<String> fileNames = multipartRequest.getFileNames();
                while (fileNames.hasNext()) {
                    String fileName = fileNames.next();
                    if (fileName == null) {
                        continue;
                    }
                    List<MultipartFile> fileParts = multipartRequest.getFiles(fileName);
                    fileList.addAll(fileParts);
                }
                files = fileList.toArray(new MultipartFile[0]);
            } else {
                // Fallback: try to use Spring's @RequestParam approach via reflection
                // This shouldn't happen, but just in case
                log.warn("Request is not StandardMultipartHttpServletRequest, using fallback");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("Unable to parse multipart request"));
            }
            
            log.info("Processing multipart submission: caseId={}, filesCount={}", 
                    caseId, files != null ? files.length : 0);
            
            ModuleFormSubmissionDTO saved = moduleFormService.submitFormWithFiles(
                    caseId, moduleType, officerId, allParams, fileMetadataJson, files, remarks);
            
            log.info("Multipart form submitted successfully: caseId={}, submissionId={}", 
                    caseId, saved.getId());
            return ResponseEntity.ok(ApiResponse.success("Field report submitted successfully", saved));
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument when submitting multipart form: caseId={}, error={}", 
                    caseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error submitting multipart form: caseId={}, error={}", 
                    caseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to submit field report: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error submitting multipart form: caseId={}, error={}", 
                    caseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred: " + e.getMessage()));
        }
    }

    /**
     * Submit field report with file uploads (multipart/form-data)
     * POST /api/cases/{caseId}/module-forms/FIELD_REPORT/submit-with-files
     * 
     * Accepts multipart/form-data with:
     * - Regular form fields (report_date, etc.)
     * - fileMetadata: JSON string with file metadata
     * - files: MultipartFile[] with actual file data
     * - fileInfo_0, fileInfo_1, ...: JSON strings with file information
     * - remarks: Optional remarks
     */
    @PostMapping(value = "/{caseId}/module-forms/FIELD_REPORT/submit-with-files", 
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ModuleFormSubmissionDTO>> submitFieldReportWithFiles(
            @PathVariable Long caseId,
            @RequestParam Map<String, String> allParams, // All form parameters
            @RequestParam(value = "fileMetadata", required = false) String fileMetadataJson,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(required = false) String remarks,
            HttpServletRequest request) {
        
        log.info("Submit field report with files: caseId={}, filesCount={}", 
                caseId, files != null ? files.length : 0);
        
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        
        Long officerId = currentUserService.getCurrentOfficerId(request);
        if (officerId == null) {
            log.warn("Officer ID not found in request for caseId={}", caseId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Officer information not found"));
        }
        
        try {
            // Process multipart form data and save files
            ModuleFormSubmissionDTO saved = moduleFormService.submitFormWithFiles(
                    caseId, "SUBMIT_FIELD_REPORT", officerId, allParams,
                    fileMetadataJson, files, remarks);
            
            log.info("Field report with files submitted successfully: caseId={}, submissionId={}", 
                    caseId, saved.getId());
            return ResponseEntity.ok(ApiResponse.success("Field report submitted successfully", saved));
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid argument when submitting field report: caseId={}, error={}", 
                    caseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid request: " + e.getMessage()));
        } catch (RuntimeException e) {
            log.error("Error submitting field report: caseId={}, error={}", 
                    caseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to submit field report: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error submitting field report: caseId={}, error={}", 
                    caseId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("An unexpected error occurred: " + e.getMessage()));
        }
    }

    /**
     * Get parties (petitioner, respondent) for a case to mark attendance
     * GET /api/cases/{caseId}/parties
     */
    @GetMapping("/{caseId}/parties")
    public ResponseEntity<ApiResponse<CasePartiesDTO>> getCaseParties(
            @PathVariable Long caseId) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        CasePartiesDTO parties = caseService.getCaseParties(caseId);
        return ResponseEntity.ok(ApiResponse.success("Case parties retrieved successfully", parties));
    }

    @GetMapping("/{caseId}/hearing-history")
    public ResponseEntity<ApiResponse<List<HearingEventDTO>>> getHearingHistory(@PathVariable Long caseId) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        return ResponseEntity.ok(ApiResponse.success("Hearing history retrieved successfully",
                moduleFormService.getHearingHistory(caseId)));
    }

    @GetMapping("/{caseId}/hearings/{hearingSubmissionId}/activity")
    public ResponseEntity<ApiResponse<HearingActivityDTO>> getHearingActivity(
            @PathVariable Long caseId,
            @PathVariable Long hearingSubmissionId) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        if (hearingSubmissionId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("hearingSubmissionId cannot be null"));
        }
        return ResponseEntity.ok(ApiResponse.success("Hearing activity retrieved successfully",
                moduleFormService.getHearingActivity(caseId, hearingSubmissionId)));
    }
}

