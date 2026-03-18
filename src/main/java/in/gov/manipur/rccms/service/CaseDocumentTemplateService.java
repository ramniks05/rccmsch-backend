package in.gov.manipur.rccms.service;

import in.gov.manipur.rccms.dto.CreateDocumentTemplateDTO;
import in.gov.manipur.rccms.dto.DocumentTemplateDTO;
import in.gov.manipur.rccms.dto.UpdateDocumentTemplateDTO;
import in.gov.manipur.rccms.entity.CaseDocumentTemplate;
import in.gov.manipur.rccms.entity.CaseNature;
import in.gov.manipur.rccms.entity.ModuleType;
import in.gov.manipur.rccms.entity.CaseType;
import in.gov.manipur.rccms.repository.CaseDocumentTemplateRepository;
import in.gov.manipur.rccms.repository.CaseNatureRepository;
import in.gov.manipur.rccms.repository.CaseTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CaseDocumentTemplateService {

    private final CaseDocumentTemplateRepository templateRepository;
    private final CaseNatureRepository caseNatureRepository;
    private final CaseTypeRepository caseTypeRepository;

    private static final List<ModuleType> DOCUMENT_MODULE_TYPES = List.of(
            ModuleType.NOTICE,
            ModuleType.ORDERSHEET,
            ModuleType.JUDGEMENT
    );

    @Transactional(readOnly = true)
    public List<DocumentTemplateDTO> getActiveTemplates(Long caseNatureId, Long caseTypeId, ModuleType moduleType) {
        if (caseNatureId == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("Module type cannot be null");
        }
        validateDocumentModuleType(moduleType);
        List<CaseDocumentTemplate> templates = caseTypeId != null
                ? templateRepository.findActiveTemplatesByCaseType(caseNatureId, caseTypeId, moduleType)
                : templateRepository.findActiveTemplates(caseNatureId, moduleType);
        if (templates.isEmpty() && caseTypeId != null) {
            templates = templateRepository.findActiveTemplates(caseNatureId, moduleType);
        }
        return templates.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentTemplateDTO> getAllTemplates(Long caseNatureId, Long caseTypeId, ModuleType moduleType) {
        if (caseNatureId == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("Module type cannot be null");
        }
        validateDocumentModuleType(moduleType);
        List<CaseDocumentTemplate> templates = caseTypeId != null
                ? templateRepository.findByCaseNatureIdAndCaseTypeIdAndModuleTypeOrderByVersionDesc(
                        caseNatureId, caseTypeId, moduleType)
                : templateRepository.findByCaseNatureIdAndModuleTypeOrderByVersionDesc(caseNatureId, moduleType);
        return templates.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentTemplateDTO getTemplate(Long templateId) {
        if (templateId == null) {
            throw new IllegalArgumentException("Template ID cannot be null");
        }
        CaseDocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
        return toDto(template);
    }

    public DocumentTemplateDTO createTemplate(CreateDocumentTemplateDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("CreateDocumentTemplateDTO cannot be null");
        }
        Long caseNatureId = dto.getCaseNatureId();
        if (caseNatureId == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        Long caseTypeId = dto.getCaseTypeId();
        CaseNature caseNature = caseNatureRepository.findById(caseNatureId)
                .orElseThrow(() -> new RuntimeException("Case nature not found: " + caseNatureId));
        validateDocumentModuleType(dto.getModuleType());
        CaseType caseType = null;
        if (caseTypeId != null) {
            caseType = caseTypeRepository.findById(caseTypeId)
                    .orElseThrow(() -> new RuntimeException("Case type not found: " + caseTypeId));
        }

        CaseDocumentTemplate template = new CaseDocumentTemplate();
        template.setCaseNature(caseNature);
        template.setCaseNatureId(caseNature.getId());
        template.setCaseType(caseType);
        template.setCaseTypeId(caseTypeId);
        template.setModuleType(dto.getModuleType());
        template.setTemplateName(dto.getTemplateName());
        template.setTemplateHtml(dto.getTemplateHtml());
        template.setTemplateData(dto.getTemplateData());
        template.setVersion(dto.getVersion() != null ? dto.getVersion() : 1);
        template.setAllowEditAfterSign(dto.getAllowEditAfterSign() != null && dto.getAllowEditAfterSign());
        template.setIsActive(dto.getIsActive() == null || dto.getIsActive());

        return toDto(templateRepository.save(template));
    }

    public DocumentTemplateDTO updateTemplate(Long templateId, UpdateDocumentTemplateDTO dto) {
        if (templateId == null) {
            throw new IllegalArgumentException("Template ID cannot be null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("UpdateDocumentTemplateDTO cannot be null");
        }
        CaseDocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
        validateDocumentModuleType(template.getModuleType());
        Long caseTypeId = dto.getCaseTypeId();
        if (caseTypeId != null) {
            CaseType caseType = caseTypeRepository.findById(caseTypeId)
                    .orElseThrow(() -> new RuntimeException("Case type not found: " + caseTypeId));
            template.setCaseType(caseType);
            template.setCaseTypeId(caseTypeId);
        } else {
            template.setCaseType(null);
            template.setCaseTypeId(null);
        }

        template.setTemplateName(dto.getTemplateName());
        template.setTemplateHtml(dto.getTemplateHtml());
        template.setTemplateData(dto.getTemplateData());
        if (dto.getVersion() != null) {
            template.setVersion(dto.getVersion());
        }
        if (dto.getAllowEditAfterSign() != null) {
            template.setAllowEditAfterSign(dto.getAllowEditAfterSign());
        }
        if (dto.getIsActive() != null) {
            template.setIsActive(dto.getIsActive());
        }

        return toDto(templateRepository.save(template));
    }

    public void deleteTemplate(Long templateId) {
        if (templateId == null) {
            throw new IllegalArgumentException("Template ID cannot be null");
        }
        CaseDocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
        if (template == null) {
            throw new RuntimeException("Template not found: " + templateId);
        }
        templateRepository.delete(template);
    }

    @Transactional(readOnly = true)
    public DocumentTemplateDTO getLatestActiveTemplate(Long caseNatureId, Long caseTypeId, ModuleType moduleType) {
        if (caseNatureId == null) {
            throw new IllegalArgumentException("Case nature ID cannot be null");
        }
        if (moduleType == null) {
            throw new IllegalArgumentException("Module type cannot be null");
        }
        validateDocumentModuleType(moduleType);
        if (caseTypeId != null) {
            DocumentTemplateDTO override = templateRepository
                    .findTopByCaseNatureIdAndCaseTypeIdAndModuleTypeAndIsActiveTrueOrderByVersionDesc(
                            caseNatureId, caseTypeId, moduleType)
                    .map(this::toDto)
                    .orElse(null);
            if (override != null) {
                return override;
            }
        }
        return templateRepository.findTopByCaseNatureIdAndModuleTypeAndIsActiveTrueOrderByVersionDesc(
                        caseNatureId, moduleType)
                .map(this::toDto)
                .orElse(null);
    }

    private DocumentTemplateDTO toDto(CaseDocumentTemplate template) {
        DocumentTemplateDTO dto = new DocumentTemplateDTO();
        dto.setId(template.getId());
        dto.setCaseNatureId(template.getCaseNatureId());
        dto.setCaseTypeId(template.getCaseTypeId());
        dto.setModuleType(template.getModuleType());
        dto.setTemplateName(template.getTemplateName());
        dto.setTemplateHtml(template.getTemplateHtml());
        dto.setTemplateData(template.getTemplateData());
        dto.setVersion(template.getVersion());
        dto.setAllowEditAfterSign(template.getAllowEditAfterSign());
        dto.setIsActive(template.getIsActive());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedAt(template.getUpdatedAt());
        if (template.getCaseNature() != null) {
            dto.setCaseNatureCode(template.getCaseNature().getCode());
            dto.setCaseNatureName(template.getCaseNature().getName());
        }
        if (template.getCaseType() != null) {
            dto.setCaseTypeCode(template.getCaseType().getTypeCode());
            dto.setCaseTypeName(template.getCaseType().getTypeName());
        }
        return dto;
    }

    private void validateDocumentModuleType(ModuleType moduleType) {
        if (moduleType == null || !DOCUMENT_MODULE_TYPES.contains(moduleType)) {
            throw new IllegalArgumentException(
                    "Invalid document module type: " + moduleType + ". Allowed: NOTICE, ORDERSHEET, JUDGEMENT");
        }
    }
}

