package in.gov.manipur.rccms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.manipur.rccms.dto.CaseDocumentDTO;
import in.gov.manipur.rccms.dto.CreateCaseDocumentDTO;
import in.gov.manipur.rccms.entity.*;
import in.gov.manipur.rccms.repository.CaseDocumentRepository;
import in.gov.manipur.rccms.repository.CaseDocumentTemplateRepository;
import in.gov.manipur.rccms.repository.CaseRepository;
import in.gov.manipur.rccms.repository.CaseWorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CaseDocumentService {

    private final CaseDocumentRepository documentRepository;
    private final CaseDocumentTemplateRepository templateRepository;
    private final CaseRepository caseRepository;
    private final CaseWorkflowInstanceRepository workflowInstanceRepository;
    private final ObjectMapper objectMapper;

    public CaseDocumentDTO createOrUpdateDocument(Long caseId, ModuleType moduleType, Long officerId, CreateCaseDocumentDTO dto, String roleCode) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("Module type cannot be null");
        }
        if (officerId == null) {
            throw new IllegalArgumentException("Officer ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("CreateCaseDocumentDTO cannot be null");
        }
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));

        CaseDocumentTemplate template = null;
        if (dto.getTemplateId() != null) {
            Long templateId = dto.getTemplateId();
            if (templateId == null) {
                throw new IllegalArgumentException("Template ID cannot be null");
            }
            template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
        }

        Optional<CaseDocument> existing = documentRepository.findTopByCaseIdAndModuleTypeOrderByUpdatedAtDesc(caseId, moduleType);
        CaseDocument doc = existing.orElseGet(CaseDocument::new);

        if (doc.getId() != null && doc.getStatus() == DocumentStatus.SIGNED) {
            boolean allowEdit = template != null && Boolean.TRUE.equals(template.getAllowEditAfterSign());
            if (!allowEdit) {
                throw new RuntimeException("Signed document cannot be edited");
            }
        }

        doc.setCaseEntity(caseEntity);
        doc.setCaseId(caseEntity.getId());
        doc.setCaseNature(caseEntity.getCaseNature());
        doc.setCaseNatureId(caseEntity.getCaseNatureId());
        doc.setModuleType(moduleType);
        if (template != null) {
            doc.setTemplate(template);
            doc.setTemplateId(template.getId());
        }
        doc.setContentHtml(dto.getContentHtml());
        doc.setContentData(dto.getContentData());
        
        // Role-based status validation
        DocumentStatus requestedStatus = dto.getStatus() != null ? dto.getStatus() : DocumentStatus.DRAFT;
        
        // READER can only save as DRAFT
        if ("READER".equals(roleCode)) {
            if (requestedStatus != DocumentStatus.DRAFT) {
                log.warn("READER role attempted to save document with status: {}. Forcing to DRAFT.", requestedStatus);
                requestedStatus = DocumentStatus.DRAFT;
            }
        }
        
        doc.setStatus(requestedStatus);

        // Only TEHSILDAR and other authorized roles can sign
        if (requestedStatus == DocumentStatus.SIGNED) {
            // Additional check: READER cannot finalize/sign
            if ("READER".equals(roleCode)) {
                throw new RuntimeException("READER role cannot finalize or sign documents. Only DRAFT status is allowed.");
            }
            doc.setSignedByOfficerId(officerId);
            doc.setSignedAt(LocalDateTime.now());
            doc.setStatus(DocumentStatus.SIGNED);
        }

        CaseDocument saved = documentRepository.save(doc);

        // Update workflow data flags: two stages - DRAFT (save as draft) and SIGNED (save and sign).
        // When signed, keep _DRAFT_CREATED true (draft was submitted) and set _SIGNED true (now signed).
        String moduleName = moduleType.name();
        if (saved.getStatus() == DocumentStatus.DRAFT) {
            updateWorkflowFlag(caseId, moduleName + "_DRAFT_CREATED", true);
            updateWorkflowFlag(caseId, moduleName + "_SIGNED", false);
        } else {
            // FINAL or SIGNED → draft was created and is now signed (both flags true)
            updateWorkflowFlag(caseId, moduleName + "_DRAFT_CREATED", true);
            updateWorkflowFlag(caseId, moduleName + "_SIGNED", true);
        }

        return toDto(saved);
    }

    /** Create or update document by template ID (resolves module type from template, finds existing by caseId+templateId). */
    public CaseDocumentDTO createOrUpdateDocumentByTemplateId(Long caseId, Long templateId, Long officerId, CreateCaseDocumentDTO dto, String roleCode) {
        CaseDocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
        if (dto == null) {
            dto = new CreateCaseDocumentDTO();
        }
        dto.setTemplateId(templateId);
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
        Optional<CaseDocument> existing = documentRepository.findTopByCaseIdAndTemplateIdOrderByUpdatedAtDesc(caseId, templateId);
        CaseDocument doc = existing.orElseGet(CaseDocument::new);
        if (doc.getId() != null && doc.getStatus() == DocumentStatus.SIGNED) {
            if (!Boolean.TRUE.equals(template.getAllowEditAfterSign())) {
                throw new RuntimeException("Signed document cannot be edited");
            }
        }
        doc.setCaseEntity(caseEntity);
        doc.setCaseId(caseEntity.getId());
        doc.setCaseNature(caseEntity.getCaseNature());
        doc.setCaseNatureId(caseEntity.getCaseNatureId());
        doc.setModuleType(template.getModuleType());
        doc.setTemplate(template);
        doc.setTemplateId(templateId);
        doc.setContentHtml(dto.getContentHtml());
        doc.setContentData(dto.getContentData());
        DocumentStatus requestedStatus = dto.getStatus() != null ? dto.getStatus() : DocumentStatus.DRAFT;
        if ("READER".equals(roleCode)) {
            if (requestedStatus != DocumentStatus.DRAFT) {
                requestedStatus = DocumentStatus.DRAFT;
            }
        }
        doc.setStatus(requestedStatus);
        if (requestedStatus == DocumentStatus.SIGNED) {
            if ("READER".equals(roleCode)) {
                throw new RuntimeException("READER role cannot finalize or sign documents. Only DRAFT status is allowed.");
            }
            doc.setSignedByOfficerId(officerId);
            doc.setSignedAt(LocalDateTime.now());
            doc.setStatus(DocumentStatus.SIGNED);
        }
        CaseDocument saved = documentRepository.save(doc);
        String moduleName = template.getModuleType().name();
        if (saved.getStatus() == DocumentStatus.DRAFT) {
            updateWorkflowFlag(caseId, moduleName + "_DRAFT_CREATED", true);
            updateWorkflowFlag(caseId, moduleName + "_SIGNED", false);
        } else {
            updateWorkflowFlag(caseId, moduleName + "_DRAFT_CREATED", true);
            updateWorkflowFlag(caseId, moduleName + "_SIGNED", true);
        }
        return toDto(saved);
    }

    public CaseDocumentDTO updateDocument(Long caseId, ModuleType moduleType, Long documentId, Long officerId, CreateCaseDocumentDTO dto, String roleCode) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("Module type cannot be null");
        }
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
        if (officerId == null) {
            throw new IllegalArgumentException("Officer ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("CreateCaseDocumentDTO cannot be null");
        }
        
        // Verify case exists
        caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
        
        CaseDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        
        // Verify document belongs to the case and module type
        if (!doc.getCaseId().equals(caseId)) {
            throw new RuntimeException("Document does not belong to case: " + caseId);
        }
        if (doc.getModuleType() != moduleType) {
            throw new RuntimeException("Document module type mismatch. Expected: " + moduleType + ", Found: " + doc.getModuleType());
        }
        
        // Check if signed document can be edited
        if (doc.getStatus() == DocumentStatus.SIGNED) {
            CaseDocumentTemplate template = doc.getTemplate() != null ? 
                    templateRepository.findById(doc.getTemplateId()).orElse(null) : null;
            boolean allowEdit = template != null && Boolean.TRUE.equals(template.getAllowEditAfterSign());
            if (!allowEdit) {
                throw new RuntimeException("Signed document cannot be edited");
            }
        }
        
        // Update template if provided
        if (dto.getTemplateId() != null) {
            CaseDocumentTemplate template = templateRepository.findById(dto.getTemplateId())
                    .orElseThrow(() -> new RuntimeException("Template not found: " + dto.getTemplateId()));
            doc.setTemplate(template);
            doc.setTemplateId(template.getId());
        }
        
        // Update document fields
        doc.setContentHtml(dto.getContentHtml());
        doc.setContentData(dto.getContentData());
        
        // Role-based status validation
        DocumentStatus requestedStatus = dto.getStatus() != null ? dto.getStatus() : doc.getStatus();
        
        // READER can only save as DRAFT
        if ("READER".equals(roleCode)) {
            if (requestedStatus != DocumentStatus.DRAFT) {
                log.warn("READER role attempted to update document with status: {}. Forcing to DRAFT.", requestedStatus);
                requestedStatus = DocumentStatus.DRAFT;
            }
        }
        
        doc.setStatus(requestedStatus);

        // Only TEHSILDAR and other authorized roles can sign
        if (requestedStatus == DocumentStatus.SIGNED) {
            // Additional check: READER cannot finalize/sign
            if ("READER".equals(roleCode)) {
                throw new RuntimeException("READER role cannot finalize or sign documents. Only DRAFT status is allowed.");
            }
            doc.setSignedByOfficerId(officerId);
            doc.setSignedAt(LocalDateTime.now());
            doc.setStatus(DocumentStatus.SIGNED);
        }
        
        CaseDocument saved = documentRepository.save(doc);
        
        // Update workflow data flags: two stages - DRAFT (save as draft) and SIGNED (save and sign).
        // When signed, keep _DRAFT_CREATED true (draft was submitted) and set _SIGNED true (now signed).
        String moduleName = moduleType.name();
        if (saved.getStatus() == DocumentStatus.DRAFT) {
            updateWorkflowFlag(caseId, moduleName + "_DRAFT_CREATED", true);
            updateWorkflowFlag(caseId, moduleName + "_SIGNED", false);
        } else {
            // FINAL or SIGNED → draft was created and is now signed (both flags true)
            updateWorkflowFlag(caseId, moduleName + "_DRAFT_CREATED", true);
            updateWorkflowFlag(caseId, moduleName + "_SIGNED", true);
        }
        
        return toDto(saved);
    }

    /** Update document by template ID; validates document belongs to case and template. */
    public CaseDocumentDTO updateDocumentByTemplateId(Long caseId, Long templateId, Long documentId, Long officerId, CreateCaseDocumentDTO dto, String roleCode) {
        CaseDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        if (!doc.getCaseId().equals(caseId)) {
            throw new RuntimeException("Document does not belong to case: " + caseId);
        }
        if (!java.util.Objects.equals(doc.getTemplateId(), templateId)) {
            throw new RuntimeException("Document template mismatch. Expected template: " + templateId);
        }
        return updateDocument(caseId, doc.getModuleType(), documentId, officerId, dto, roleCode);
    }

    @Transactional(readOnly = true)
    public CaseDocumentDTO getLatestDocument(Long caseId, ModuleType moduleType) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("Module type cannot be null");
        }
        return documentRepository.findTopByCaseIdAndModuleTypeOrderByUpdatedAtDesc(caseId, moduleType)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public CaseDocumentDTO getLatestDocumentByTemplateId(Long caseId, Long templateId) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        if (templateId == null) {
            throw new IllegalArgumentException("Template ID cannot be null");
        }
        return documentRepository.findTopByCaseIdAndTemplateIdOrderByUpdatedAtDesc(caseId, templateId)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * Get all documents of a specific type for a case
     */
    @Transactional(readOnly = true)
    public List<CaseDocumentDTO> getAllDocuments(Long caseId, ModuleType moduleType) {
        if (caseId == null) {
            throw new IllegalArgumentException("Case ID cannot be null");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("Module type cannot be null");
        }
        List<CaseDocument> documents = documentRepository.findByCaseIdAndModuleTypeOrderByUpdatedAtDesc(caseId, moduleType);
        return documents.stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
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

    private CaseDocumentDTO toDto(CaseDocument doc) {
        CaseDocumentDTO dto = new CaseDocumentDTO();
        dto.setId(doc.getId());
        dto.setCaseId(doc.getCaseId());
        dto.setCaseNatureId(doc.getCaseNatureId());
        dto.setModuleType(doc.getModuleType());
        dto.setTemplateId(doc.getTemplateId());
        if (doc.getTemplate() != null) {
            dto.setTemplateName(doc.getTemplate().getTemplateName());
        }
        dto.setContentHtml(doc.getContentHtml());
        dto.setContentData(doc.getContentData());
        dto.setStatus(doc.getStatus());
        dto.setSignedByOfficerId(doc.getSignedByOfficerId());
        dto.setSignedAt(doc.getSignedAt());
        dto.setCreatedAt(doc.getCreatedAt());
        dto.setUpdatedAt(doc.getUpdatedAt());
        return dto;
    }
}

