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

    public CaseDocumentDTO createOrUpdateDocument(Long caseId, ModuleType moduleType, Long officerId, CreateCaseDocumentDTO dto) {
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
        DocumentStatus requestedStatus = dto.getStatus() != null ? dto.getStatus() : DocumentStatus.DRAFT;
        doc.setStatus(requestedStatus);

        // Finalize = sign: when officer finalizes (FINAL), treat as finalize + digital signature in one action
        if (requestedStatus == DocumentStatus.FINAL || requestedStatus == DocumentStatus.SIGNED) {
            doc.setSignedByOfficerId(officerId);
            doc.setSignedAt(LocalDateTime.now());
            doc.setStatus(DocumentStatus.SIGNED); // persist as SIGNED so document is both finalized and signed
        }

        CaseDocument saved = documentRepository.save(doc);

        // Update workflow data flags based on document status
        // *_SIGNED is set only when document is SIGNED (finalize). Never set for DRAFT — drafting never requires signature.
        String moduleName = moduleType.name();
        
        if (saved.getStatus() == DocumentStatus.DRAFT) {
            updateWorkflowFlag(caseId, moduleName + "_DRAFT_CREATED", true);
            updateWorkflowFlag(caseId, moduleName + "_READY", false);
            updateWorkflowFlag(caseId, moduleName + "_SIGNED", false);
        } else if (saved.getStatus() == DocumentStatus.SIGNED) {
            updateWorkflowFlag(caseId, moduleName + "_READY", true);
            updateWorkflowFlag(caseId, moduleName + "_SIGNED", true); // only set when finalized/signed
        }

        return toDto(saved);
    }

    public CaseDocumentDTO updateDocument(Long caseId, ModuleType moduleType, Long documentId, Long officerId, CreateCaseDocumentDTO dto) {
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
        
        Case caseEntity = caseRepository.findById(caseId)
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
        DocumentStatus requestedStatus = dto.getStatus() != null ? dto.getStatus() : doc.getStatus();
        doc.setStatus(requestedStatus);

        // Finalize = sign: when officer finalizes (FINAL), treat as finalize + digital signature in one action
        if (requestedStatus == DocumentStatus.FINAL || requestedStatus == DocumentStatus.SIGNED) {
            doc.setSignedByOfficerId(officerId);
            doc.setSignedAt(LocalDateTime.now());
            doc.setStatus(DocumentStatus.SIGNED);
        }
        
        CaseDocument saved = documentRepository.save(doc);
        
        // Update workflow data flags. *_SIGNED only when document is SIGNED (finalize). Not set for DRAFT.
        String moduleName = moduleType.name();
        
        if (saved.getStatus() == DocumentStatus.DRAFT) {
            updateWorkflowFlag(caseId, moduleName + "_DRAFT_CREATED", true);
            updateWorkflowFlag(caseId, moduleName + "_READY", false);
            updateWorkflowFlag(caseId, moduleName + "_SIGNED", false);
        } else if (saved.getStatus() == DocumentStatus.SIGNED) {
            updateWorkflowFlag(caseId, moduleName + "_READY", true);
            updateWorkflowFlag(caseId, moduleName + "_SIGNED", true);
        }
        
        return toDto(saved);
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

