package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.*;
import in.gov.manipur.rccms.entity.*;
import in.gov.manipur.rccms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for case module forms (hearing, notice, ordersheet, judgement)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CaseModuleFormService {

    private final CaseModuleFormFieldRepository fieldRepository;
    private final CaseModuleFormSubmissionRepository submissionRepository;
    private final CaseRepository caseRepository;
    private final CaseNatureRepository caseNatureRepository;
    private final CaseTypeRepository caseTypeRepository;
    private final CaseWorkflowInstanceRepository workflowInstanceRepository;
    private final ModuleMasterService moduleMasterService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ModuleFormSchemaDTO getFormSchema(Long caseNatureId, Long caseTypeId, String moduleType) {
        if (caseNatureId == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        String moduleCode = moduleMasterService.requireActiveModuleCode(moduleType);
        CaseNature caseNature = caseNatureRepository.findById(caseNatureId)
                .orElseThrow(() -> new RuntimeException("Case nature not found: " + caseNatureId));

        CaseType caseType = null;
        if (caseTypeId != null) {
            caseType = caseTypeRepository.findById(caseTypeId).orElse(null);
        }

        List<CaseModuleFormFieldDefinition> fields = new ArrayList<>();
        if (caseTypeId != null) {
            fields = fieldRepository.findActiveFieldsByCaseType(caseNatureId, caseTypeId, moduleCode);
        }
        if (fields.isEmpty()) {
            fields = fieldRepository.findActiveFields(caseNatureId, moduleCode);
        }
        List<ModuleFormFieldDTO> fieldDTOs = fields.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ModuleFormSchemaDTO.builder()
                .caseNatureId(caseNatureId)
                .caseNatureCode(caseNature.getCode())
                .caseNatureName(caseNature.getName())
                .caseTypeId(caseTypeId)
                .caseTypeCode(caseType != null ? caseType.getTypeCode() : null)
                .caseTypeName(caseType != null ? caseType.getTypeName() : null)
                .moduleType(moduleCode)
                .fields(fieldDTOs)
                .totalFields(fieldDTOs.size())
                .build();
    }

    /**
     * Convenience helper: get module form schema + latest data for a case,
     * resolving moduleType from a module form group id (field definition id used in permission-forms).
     */
    @Transactional(readOnly = true)
    public ModuleFormWithDataDTO getModuleFormDataByFormId(Long caseId, Long formId) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        if (formId == null) {
            throw new IllegalArgumentException("Form ID cannot be null");
        }

        CaseModuleFormFieldDefinition fieldDef = fieldRepository.findById(formId)
                .orElseThrow(() -> new RuntimeException("Module form definition not found: " + formId));

        String moduleType = fieldDef.getModuleType();

        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        ModuleFormSchemaDTO schema = getFormSchema(
                caseEntity.getCaseNatureId(), caseEntity.getCaseTypeId(), moduleType);

        Optional<ModuleFormSubmissionDTO> submission = getLatestSubmission(caseId, moduleType);

        Map<String, Object> formData = null;
        boolean hasExistingData = false;

        if (submission.isPresent() && submission.get().getFormData() != null) {
            try {
                formData = objectMapper.readValue(submission.get().getFormData(),
                        new TypeReference<Map<String, Object>>() {});
                hasExistingData = true;
            } catch (Exception e) {
                log.error("Error parsing form data for case {} module {} (formId {}): {}",
                        caseId, moduleType, formId, e.getMessage());
            }
        }

        return ModuleFormWithDataDTO.builder()
                .schema(schema)
                .formData(formData)
                .hasExistingData(hasExistingData)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ModuleFormFieldDTO> getAllFields(Long caseNatureId, Long caseTypeId, String moduleType) {
        if (caseNatureId == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        String moduleCode = moduleMasterService.requireActiveModuleCode(moduleType);
        List<CaseModuleFormFieldDefinition> fields;
        if (caseTypeId != null) {
            fields = fieldRepository.findAllFieldsByCaseType(caseNatureId, caseTypeId, moduleCode);
        } else {
            fields = fieldRepository.findAllFields(caseNatureId, moduleCode);
        }
        return fields.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ModuleFormFieldDTO createField(CreateModuleFormFieldDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("CreateModuleFormFieldDTO cannot be null");
        }
        Long caseNatureId = dto.getCaseNatureId();
        if (caseNatureId == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        Long caseTypeId = dto.getCaseTypeId();
        CaseNature caseNature = caseNatureRepository.findById(caseNatureId)
                .orElseThrow(() -> new RuntimeException("Case nature not found: " + caseNatureId));
        CaseType caseType = null;
        if (caseTypeId != null) {
            caseType = caseTypeRepository.findById(caseTypeId)
                    .orElseThrow(() -> new RuntimeException("Case type not found: " + caseTypeId));
        }

        String moduleType = moduleMasterService.requireActiveModuleCode(dto.getModuleType());

        if (fieldRepository.existsByCaseNatureIdAndCaseTypeIdAndModuleTypeAndFieldName(
                caseNatureId, caseTypeId, moduleType, dto.getFieldName())) {
            throw new RuntimeException("Field name already exists for this case nature and module type");
        }

        CaseModuleFormFieldDefinition field = new CaseModuleFormFieldDefinition();
        field.setCaseNature(caseNature);
        field.setCaseNatureId(caseNature.getId());
        field.setCaseType(caseType);
        field.setCaseTypeId(caseTypeId);
        field.setModuleType(moduleType);
        field.setFieldName(dto.getFieldName());
        field.setFieldLabel(dto.getFieldLabel());
        field.setFieldType(dto.getFieldType());
        field.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : false);
        field.setValidationRules(dto.getValidationRules());
        field.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        field.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        field.setDefaultValue(dto.getDefaultValue());
        field.setFieldOptions(dto.getFieldOptions());
        field.setItemSchema(dto.getItemSchema());
        field.setPlaceholder(dto.getPlaceholder());
        field.setHelpText(dto.getHelpText());
        field.setDataSource(dto.getDataSource());
        field.setDependsOnField(dto.getDependsOnField());
        field.setDependencyCondition(dto.getDependencyCondition());
        field.setConditionalLogic(dto.getConditionalLogic());

        return toDto(fieldRepository.save(field));
    }

    public ModuleFormFieldDTO updateField(Long fieldId, UpdateModuleFormFieldDTO dto) {
        if (fieldId == null) {
            throw new IllegalArgumentException("Field ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("UpdateModuleFormFieldDTO cannot be null");
        }
        CaseModuleFormFieldDefinition field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Module form field not found: " + fieldId));

        String moduleType = dto.getModuleType();
        if (moduleType != null) {
            field.setModuleType(moduleMasterService.requireActiveModuleCode(moduleType));
        }

        Long caseTypeId = dto.getCaseTypeId();
        if (caseTypeId != null) {
            CaseType caseType = caseTypeRepository.findById(caseTypeId)
                    .orElseThrow(() -> new RuntimeException("Case type not found: " + caseTypeId));
            field.setCaseType(caseType);
            field.setCaseTypeId(caseTypeId);
        } else {
            field.setCaseType(null);
            field.setCaseTypeId(null);
        }
        field.setFieldName(dto.getFieldName());
        field.setFieldLabel(dto.getFieldLabel());
        field.setFieldType(dto.getFieldType());
        field.setIsRequired(dto.getIsRequired() != null ? dto.getIsRequired() : field.getIsRequired());
        field.setValidationRules(dto.getValidationRules());
        field.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : field.getDisplayOrder());
        field.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : field.getIsActive());
        field.setDefaultValue(dto.getDefaultValue());
        field.setFieldOptions(dto.getFieldOptions());
        field.setItemSchema(dto.getItemSchema());
        field.setPlaceholder(dto.getPlaceholder());
        field.setHelpText(dto.getHelpText());
        field.setDataSource(dto.getDataSource());
        field.setDependsOnField(dto.getDependsOnField());
        field.setDependencyCondition(dto.getDependencyCondition());
        field.setConditionalLogic(dto.getConditionalLogic());

        return toDto(fieldRepository.save(field));
    }

    public void deleteField(Long fieldId) {
        if (fieldId == null) {
            throw new IllegalArgumentException("Field ID cannot be null");
        }
        CaseModuleFormFieldDefinition field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("Module form field not found: " + fieldId));
        if (field == null) {
            throw new RuntimeException("Module form field not found: " + fieldId);
        }
        fieldRepository.delete(field);
    }

    public ModuleFormSubmissionDTO submitForm(Long caseId, String moduleType, Long officerId, CreateModuleFormSubmissionDTO dto) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        String moduleCode = moduleMasterService.requireActiveModuleCode(moduleType);
        if (officerId == null) {
            throw new IllegalArgumentException("Officer ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("CreateModuleFormSubmissionDTO cannot be null");
        }
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        // Process formData to save any files and update file paths
        String processedFormData = processFormDataFiles(dto.getFormData(), caseId, moduleCode);

        CaseModuleFormSubmission submission = new CaseModuleFormSubmission();
        submission.setCaseEntity(caseEntity);
        submission.setCaseId(caseEntity.getId());
        submission.setCaseNature(caseEntity.getCaseNature());
        submission.setCaseNatureId(caseEntity.getCaseNatureId());
        submission.setModuleType(moduleCode);
        submission.setFormData(processedFormData);
        submission.setRemarks(dto.getRemarks());
        submission.setSubmittedByOfficerId(officerId);

        CaseModuleFormSubmission saved = submissionRepository.save(submission);

        // Update workflow data flag for checklist
        updateWorkflowFlag(caseId, moduleCode + "_SUBMITTED", true);

        return toSubmissionDto(saved);
    }

    /**
     * Submit form with file uploads (multipart/form-data)
     * Processes files, saves them to disk, and creates formData with file URLs
     */
    public ModuleFormSubmissionDTO submitFormWithFiles(
            Long caseId, 
            String moduleType, 
            Long officerId,
            Map<String, String> allParams,
            String fileMetadataJson,
            MultipartFile[] files,
            String remarks) {
        
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        String moduleCode = moduleMasterService.requireActiveModuleCode(moduleType);
        if (officerId == null) {
            throw new IllegalArgumentException("Officer ID cannot be null");
        }
        
        // Verify case exists
        caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
        
        log.info("Processing field report submission with files: caseId={}, filesCount={}", 
                caseId, files != null ? files.length : 0);
        
        // 1. Parse file metadata
        Map<String, List<FileMetadataDTO>> fileMetadataMap = new HashMap<>();
        if (fileMetadataJson != null && !fileMetadataJson.trim().isEmpty()) {
            try {
                fileMetadataMap = objectMapper.readValue(fileMetadataJson, 
                        new TypeReference<Map<String, List<FileMetadataDTO>>>() {});
                log.debug("Parsed file metadata: {}", fileMetadataMap);
            } catch (Exception e) {
                log.error("Error parsing fileMetadata JSON: {}", e.getMessage(), e);
                throw new IllegalArgumentException("Invalid fileMetadata JSON: " + e.getMessage());
            }
        }
        
        // 2. Extract fileInfo entries from allParams
        Map<Integer, FileInfoDTO> fileInfoMap = new HashMap<>();
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().startsWith("fileInfo_")) {
                try {
                    String indexStr = entry.getKey().substring("fileInfo_".length());
                    int index = Integer.parseInt(indexStr);
                    FileInfoDTO fileInfo = objectMapper.readValue(entry.getValue(), FileInfoDTO.class);
                    fileInfoMap.put(index, fileInfo);
                    log.debug("Parsed fileInfo_{}: {}", index, fileInfo);
                } catch (Exception e) {
                    log.error("Error parsing fileInfo entry {}: {}", entry.getKey(), e.getMessage());
                }
            }
        }
        
        // 3. Process files and save them
        List<SavedFileInfo> savedFiles = new ArrayList<>();
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (file == null || file.isEmpty()) {
                    log.warn("Skipping empty file at index {}", i);
                    continue;
                }
                
                // Get file info for this index
                FileInfoDTO fileInfo = fileInfoMap.get(i);
                if (fileInfo == null) {
                    log.warn("No fileInfo found for file index {}, using default", i);
                    fileInfo = new FileInfoDTO();
                    fileInfo.setFieldName("supporting_documents");
                    fileInfo.setFileId("temp-" + i);
                    fileInfo.setDisplayName(file.getOriginalFilename());
                    fileInfo.setOriginalFileName(file.getOriginalFilename());
                }
                
                try {
                    // Generate unique file ID
                    String fileId = UUID.randomUUID().toString();
                    
                    // Determine storage path: /uploads/documents/case-{caseId}/{fileId}_{originalFileName}
                    String originalFileName = fileInfo.getOriginalFileName() != null ? 
                            fileInfo.getOriginalFileName() : file.getOriginalFilename();
                    String sanitizedFileName = sanitizeFileName(originalFileName);
                    
                    // Get absolute path to uploads directory
                    Path uploadsBasePath = getUploadsBasePath();
                    Path caseDirPath = uploadsBasePath.resolve("documents").resolve("case-" + caseId);
                    Path filePath = caseDirPath.resolve(fileId + "_" + sanitizedFileName);
                    
                    // Create directories if they don't exist
                    Files.createDirectories(caseDirPath);
                    
                    // Save file
                    Files.write(filePath, file.getBytes());
                    
                    String fileUrl = "/uploads/documents/case-" + caseId + "/" + fileId + "_" + sanitizedFileName;
                    
                    log.info("Saved file to absolute path: {}", filePath.toAbsolutePath());
                    
                    // Store saved file info
                    SavedFileInfo savedFile = SavedFileInfo.builder()
                            .fileId(fileId)
                            .originalFileId(fileInfo.getFileId())
                            .fieldName(fileInfo.getFieldName())
                            .displayName(fileInfo.getDisplayName())
                            .originalFileName(originalFileName)
                            .fileUrl(fileUrl)
                            .fileSize(file.getSize())
                            .fileType(file.getContentType())
                            .build();
                    
                    savedFiles.add(savedFile);
                    log.info("Saved file {} to {}", originalFileName, fileUrl);
                    
                } catch (Exception e) {
                    log.error("Error saving file {}: {}", file.getOriginalFilename(), e.getMessage(), e);
                    throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
                }
            }
        }
        
        // 4. Build formData object with saved file URLs
        Map<String, Object> formData = new HashMap<>();
        
        // Add regular form fields (exclude file-related fields)
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("fileInfo_") && 
                !key.equals("fileMetadata") && 
                !key.equals("files") && 
                !key.equals("remarks")) {
                formData.put(key, entry.getValue());
            }
        }
        
        // Add file fields with saved file URLs
        for (Map.Entry<String, List<FileMetadataDTO>> entry : fileMetadataMap.entrySet()) {
            String fieldName = entry.getKey();
            List<FileMetadataDTO> metadataList = entry.getValue();
            
            List<Map<String, Object>> fileList = new ArrayList<>();
            for (FileMetadataDTO meta : metadataList) {
                // Find corresponding saved file by originalFileId
                SavedFileInfo savedFile = savedFiles.stream()
                        .filter(f -> f.getOriginalFileId() != null && 
                                   f.getOriginalFileId().equals(meta.getFileId()))
                        .findFirst()
                        .orElse(null);
                
                if (savedFile != null) {
                    Map<String, Object> fileData = new HashMap<>();
                    fileData.put("fileId", savedFile.getFileId());
                    fileData.put("fileName", savedFile.getDisplayName());
                    fileData.put("fileUrl", savedFile.getFileUrl());
                    fileData.put("fileSize", savedFile.getFileSize());
                    fileData.put("fileType", savedFile.getFileType());
                    fileData.put("originalFileName", savedFile.getOriginalFileName());
                    fileList.add(fileData);
                } else {
                    log.warn("No saved file found for metadata fileId: {}", meta.getFileId());
                }
            }
            formData.put(fieldName, fileList);
        }
        
        // 5. Convert formData to JSON string
        String formDataJson;
        try {
            formDataJson = objectMapper.writeValueAsString(formData);
            log.debug("FormData JSON: {}", formDataJson);
        } catch (Exception e) {
            log.error("Error converting formData to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize form data: " + e.getMessage(), e);
        }
        
        // 6. Create submission DTO and save
        CreateModuleFormSubmissionDTO submissionDto = new CreateModuleFormSubmissionDTO();
        submissionDto.setFormData(formDataJson);
        submissionDto.setRemarks(remarks);
        
        return submitForm(caseId, moduleCode, officerId, submissionDto);
    }
    
    /**
     * Get the base path for uploads directory
     * Resolves relative to project root (current working directory)
     */
    private Path getUploadsBasePath() {
        // Try to resolve relative to current working directory
        Path uploadsPath = Paths.get("uploads").toAbsolutePath();
        
        // If directory doesn't exist, create it
        if (!Files.exists(uploadsPath)) {
            try {
                Files.createDirectories(uploadsPath);
                log.info("Created uploads directory at: {}", uploadsPath);
            } catch (Exception e) {
                log.warn("Could not create uploads directory at {}, using current directory", uploadsPath, e);
                // Fallback to current directory
                uploadsPath = Paths.get(System.getProperty("user.dir"), "uploads");
                try {
                    Files.createDirectories(uploadsPath);
                } catch (Exception ex) {
                    log.error("Failed to create uploads directory: {}", ex.getMessage(), ex);
                    throw new RuntimeException("Failed to create uploads directory", ex);
                }
            }
        }
        
        return uploadsPath;
    }
    
    /**
     * Sanitize filename to remove invalid characters
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "file";
        }
        // Replace invalid characters with underscore
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Process formData JSON to save files and update file paths
     * Handles file objects in formData (base64 encoded or file references)
     */
    private String processFormDataFiles(String formDataJson, Long caseId, String moduleType) {
        if (formDataJson == null || formDataJson.trim().isEmpty()) {
            return formDataJson;
        }

        try {
            Map<String, Object> formData = objectMapper.readValue(formDataJson, 
                    new TypeReference<Map<String, Object>>() {});
            
            // Process each field in formData
            processFormDataRecursive(formData, caseId, moduleType);
            
            // Convert back to JSON
            return objectMapper.writeValueAsString(formData);
        } catch (Exception e) {
            log.error("Error processing form data files for case {} module {}: {}", 
                    caseId, moduleType, e.getMessage(), e);
            // Return original formData if processing fails
            return formDataJson;
        }
    }

    /**
     * Recursively process formData to find and save files
     */
    @SuppressWarnings("unchecked")
    private void processFormDataRecursive(Object value, Long caseId, String moduleType) {
        if (value == null) {
            return;
        }

        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            
            // Check if this is a file object (has fileUrl, fileData, or similar)
            if (map.containsKey("fileUrl") || map.containsKey("fileData") || 
                map.containsKey("fileName") || map.containsKey("file")) {
                processFileObject(map, caseId, moduleType);
            } else {
                // Recursively process nested objects
                for (Object nestedValue : map.values()) {
                    processFormDataRecursive(nestedValue, caseId, moduleType);
                }
            }
        } else if (value instanceof List) {
            // Process list items
            List<Object> list = (List<Object>) value;
            for (Object item : list) {
                processFormDataRecursive(item, caseId, moduleType);
            }
        }
    }

    /**
     * Process a file object and save the file if needed
     * Handles various file object formats:
     * - {fileUrl: "...", fileName: "...", fileData: "data:..."}
     * - {fileName: "...", fileUrl: "...", fileSize: ..., fileType: "..."}
     * - {file: {fileName: "...", fileData: "data:..."}}
     */
    @SuppressWarnings("unchecked")
    private void processFileObject(Map<String, Object> fileObj, Long caseId, String moduleType) {
        try {
            String fileUrl = getStringValue(fileObj, "fileUrl");
            String fileName = getStringValue(fileObj, "fileName");
            String fileData = getStringValue(fileObj, "fileData"); // Base64 encoded
            Object fileObjValue = fileObj.get("file");
            
            // If fileUrl already exists and is a valid path, verify file exists
            if (fileUrl != null && !fileUrl.isEmpty()) {
                if (fileUrl.startsWith("/uploads/") || fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                    // Check if local file exists
                    if (fileUrl.startsWith("/uploads/")) {
                        String localPath = fileUrl.substring(1); // Remove leading /
                        Path filePath = Paths.get(localPath);
                        if (Files.exists(filePath)) {
                            log.debug("File already exists at: {}", fileUrl);
                            return;
                        } else {
                            log.warn("File URL references non-existent file: {}", fileUrl);
                            // Continue to try to save if fileData is available
                        }
                    } else {
                        log.debug("File has external URL: {}", fileUrl);
                        return;
                    }
                }
            }
            
            // If fileData is provided (base64), save it
            if (fileData != null && !fileData.isEmpty() && fileData.startsWith("data:")) {
                String savedPath = saveBase64File(fileData, fileName, caseId, moduleType);
                fileObj.put("fileUrl", savedPath);
                fileObj.remove("fileData"); // Remove base64 data after saving
                log.info("Saved base64 file to: {}", savedPath);
            } 
            // If file object contains file info, try to save
            else if (fileObjValue instanceof Map) {
                Map<String, Object> fileMap = (Map<String, Object>) fileObjValue;
                String fName = getStringValue(fileMap, "fileName");
                String fData = getStringValue(fileMap, "fileData");
                if (fData != null && fData.startsWith("data:")) {
                    String savedPath = saveBase64File(fData, fName != null ? fName : fileName, caseId, moduleType);
                    fileObj.put("fileUrl", savedPath);
                    // Keep file object but update fileUrl
                    fileMap.put("fileUrl", savedPath);
                    fileMap.remove("fileData");
                    log.info("Saved file from nested file object to: {}", savedPath);
                }
            }
            // If fileName exists but no fileData and no valid fileUrl, log warning
            else if (fileName != null && !fileName.isEmpty() && (fileUrl == null || fileUrl.isEmpty())) {
                log.warn("File object has fileName '{}' but no fileData or fileUrl. File may need to be uploaded separately.", fileName);
            }
        } catch (Exception e) {
            log.error("Error processing file object: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Safely get string value from map
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Save a base64 encoded file to disk
     */
    private String saveBase64File(String base64Data, String originalFileName, Long caseId, String moduleType) {
        try {
            // Parse base64 data URL (format: data:image/png;base64,...)
            String[] parts = base64Data.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid base64 data format");
            }
            
            String base64Content = parts[1];
            byte[] fileBytes = Base64.getDecoder().decode(base64Content);
            
            // Generate file name
            String fileName;
            if (originalFileName != null && !originalFileName.isEmpty()) {
                // Sanitize filename
                String sanitized = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                fileName = System.currentTimeMillis() + "_" + sanitized;
            } else {
                fileName = System.currentTimeMillis() + "_file";
            }
            
            // Get absolute path to uploads directory
            Path uploadsBasePath = getUploadsBasePath();
            Path documentsDirPath = uploadsBasePath.resolve("documents");
            Path filePath = documentsDirPath.resolve(fileName);
            
            // Create directories if they don't exist
            Files.createDirectories(documentsDirPath);
            
            // Save file
            Files.write(filePath, fileBytes);
            
            String fileUrl = "/uploads/documents/" + fileName;
            log.info("Saved file: {} ({} bytes) for case {} module {} at absolute path: {}", 
                    fileName, fileBytes.length, caseId, moduleType, filePath.toAbsolutePath());
            
            return fileUrl;
        } catch (Exception e) {
            log.error("Error saving base64 file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<ModuleFormSubmissionDTO> getLatestSubmission(Long caseId, String moduleType) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        String moduleCode = moduleMasterService.requireActiveModuleCode(moduleType);
        return submissionRepository.findTopByCaseIdAndModuleTypeOrderBySubmittedAtDesc(caseId, moduleCode)
                .map(this::toSubmissionDto);
    }

    private void updateWorkflowFlag(Long caseId, String key, boolean value) {
        workflowInstanceRepository.findByCaseId(caseId).ifPresent(instance -> {
            Map<String, Object> data = parseJsonMap(instance.getWorkflowData());
            data.put(key, value);
            try {
                instance.setWorkflowData(objectMapper.writeValueAsString(data));
                workflowInstanceRepository.save(instance);
            } catch (Exception e) {
                log.error("Failed to update workflow data for case {}: {}", caseId, e.getMessage());
            }
        });
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Invalid workflow data JSON: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    private ModuleFormFieldDTO toDto(CaseModuleFormFieldDefinition field) {
        ModuleFormFieldDTO dto = new ModuleFormFieldDTO();
        dto.setId(field.getId());
        dto.setCaseNatureId(field.getCaseNatureId());
        dto.setCaseTypeId(field.getCaseTypeId());
        dto.setModuleType(field.getModuleType());
        dto.setFieldName(field.getFieldName());
        dto.setFieldLabel(field.getFieldLabel());
        dto.setFieldType(field.getFieldType());
        dto.setIsRequired(field.getIsRequired());
        dto.setValidationRules(field.getValidationRules());
        dto.setDisplayOrder(field.getDisplayOrder());
        dto.setIsActive(field.getIsActive());
        dto.setDefaultValue(field.getDefaultValue());
        dto.setFieldOptions(field.getFieldOptions());
        dto.setItemSchema(field.getItemSchema());
        dto.setPlaceholder(field.getPlaceholder());
        dto.setHelpText(field.getHelpText());
        dto.setDataSource(field.getDataSource());
        dto.setDependsOnField(field.getDependsOnField());
        dto.setDependencyCondition(field.getDependencyCondition());
        dto.setConditionalLogic(field.getConditionalLogic());
        dto.setCreatedAt(field.getCreatedAt());
        dto.setUpdatedAt(field.getUpdatedAt());
        if (field.getCaseNature() != null) {
            dto.setCaseNatureCode(field.getCaseNature().getCode());
            dto.setCaseNatureName(field.getCaseNature().getName());
        }
        if (field.getCaseType() != null) {
            dto.setCaseTypeCode(field.getCaseType().getTypeCode());
            dto.setCaseTypeName(field.getCaseType().getTypeName());
        }
        return dto;
    }

    private ModuleFormSubmissionDTO toSubmissionDto(CaseModuleFormSubmission submission) {
        ModuleFormSubmissionDTO dto = new ModuleFormSubmissionDTO();
        dto.setId(submission.getId());
        dto.setCaseId(submission.getCaseId());
        dto.setCaseNatureId(submission.getCaseNatureId());
        dto.setModuleType(submission.getModuleType());
        dto.setFormData(submission.getFormData());
        dto.setSubmittedByOfficerId(submission.getSubmittedByOfficerId());
        dto.setSubmittedAt(submission.getSubmittedAt());
        dto.setRemarks(submission.getRemarks());
        return dto;
    }
}

