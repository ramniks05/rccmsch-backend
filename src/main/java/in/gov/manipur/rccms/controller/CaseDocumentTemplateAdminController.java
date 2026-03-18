package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CreateDocumentTemplateDTO;
import in.gov.manipur.rccms.dto.DocumentTemplateDTO;
import in.gov.manipur.rccms.dto.UpdateDocumentTemplateDTO;
import in.gov.manipur.rccms.entity.ModuleType;
import in.gov.manipur.rccms.service.CaseDocumentTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin APIs for document templates (notice, ordersheet, judgement)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/document-templates")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class CaseDocumentTemplateAdminController {

    private final CaseDocumentTemplateService templateService;

    @GetMapping("/module-types")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getDocumentModuleTypes() {
        List<Map<String, String>> data = List.of(
                Map.of("code", ModuleType.NOTICE.name(), "name", "NOTICE"),
                Map.of("code", ModuleType.ORDERSHEET.name(), "name", "ORDERSHEET"),
                Map.of("code", ModuleType.JUDGEMENT.name(), "name", "JUDGEMENT")
        );
        return ResponseEntity.ok(ApiResponse.success("Document module types retrieved", data));
    }

    @GetMapping("/case-natures/{caseNatureId}/modules/{moduleType}")
    public ResponseEntity<ApiResponse<List<DocumentTemplateDTO>>> getTemplates(
            @PathVariable Long caseNatureId,
            @PathVariable ModuleType moduleType,
            @RequestParam(value = "caseTypeId", required = false) Long caseTypeId,
            @RequestParam(value = "activeOnly", defaultValue = "true") boolean activeOnly) {
        List<DocumentTemplateDTO> templates = activeOnly
                ? templateService.getActiveTemplates(caseNatureId, caseTypeId, moduleType)
                : templateService.getAllTemplates(caseNatureId, caseTypeId, moduleType);
        return ResponseEntity.ok(ApiResponse.success("Templates retrieved", templates));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<ApiResponse<DocumentTemplateDTO>> getTemplate(@PathVariable Long templateId) {
        DocumentTemplateDTO template = templateService.getTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("Template retrieved", template));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DocumentTemplateDTO>> createTemplate(
            @Valid @RequestBody CreateDocumentTemplateDTO dto) {
        DocumentTemplateDTO template = templateService.createTemplate(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Template created", template));
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<ApiResponse<DocumentTemplateDTO>> updateTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody UpdateDocumentTemplateDTO dto) {
        DocumentTemplateDTO template = templateService.updateTemplate(templateId, dto);
        return ResponseEntity.ok(ApiResponse.success("Template updated", template));
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long templateId) {
        templateService.deleteTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("Template deleted", null));
    }
}

