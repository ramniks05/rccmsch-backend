package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseDocumentDTO;
import in.gov.manipur.rccms.dto.CreateCaseDocumentDTO;
import in.gov.manipur.rccms.dto.DocumentTemplateDTO;
import in.gov.manipur.rccms.repository.CaseRepository;
import in.gov.manipur.rccms.service.CaseDocumentService;
import in.gov.manipur.rccms.service.CaseDocumentTemplateService;
import in.gov.manipur.rccms.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * APIs for case documents (notice, ordersheet, judgement)
 */
@Slf4j
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseDocumentController {

    private final CaseDocumentService documentService;
    private final CaseDocumentTemplateService templateService;
    private final CaseRepository caseRepository;
    private final CurrentUserService currentUserService;

    @GetMapping("/{caseId}/documents/{templateId}/template")
    public ResponseEntity<ApiResponse<DocumentTemplateDTO>> getActiveTemplate(
            @PathVariable Long caseId,
            @PathVariable Long templateId) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        if (templateId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Template ID cannot be null"));
        }
        caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
        DocumentTemplateDTO template = templateService.getTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.success("Template retrieved", template));
    }

    @GetMapping("/{caseId}/documents/{templateId}")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> getDocument(
            @PathVariable Long caseId,
            @PathVariable Long templateId) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        if (templateId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Template ID cannot be null"));
        }
        CaseDocumentDTO doc = documentService.getLatestDocumentByTemplateId(caseId, templateId);
        return ResponseEntity.ok(ApiResponse.success("Document retrieved", doc));
    }

    @GetMapping("/{caseId}/documents/{templateId}/latest")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> getLatestDocument(
            @PathVariable Long caseId,
            @PathVariable Long templateId) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        if (templateId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Template ID cannot be null"));
        }
        CaseDocumentDTO doc = documentService.getLatestDocumentByTemplateId(caseId, templateId);
        return ResponseEntity.ok(ApiResponse.success("Document retrieved", doc));
    }

    @PostMapping("/{caseId}/documents/{templateId}")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> createOrUpdateDocument(
            @PathVariable Long caseId,
            @PathVariable Long templateId,
            @Valid @RequestBody CreateCaseDocumentDTO dto,
            HttpServletRequest request) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        if (templateId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Template ID cannot be null"));
        }
        Long officerId = currentUserService.getCurrentOfficerId(request);
        if (officerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Officer information not found"));
        }
        String roleCode = currentUserService.getCurrentRoleCode(request);
        if (roleCode == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Role information not found"));
        }
        try {
            CaseDocumentDTO saved = documentService.createOrUpdateDocumentByTemplateId(caseId, templateId, officerId, dto, roleCode);
            return ResponseEntity.ok(ApiResponse.success("Document saved", saved));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("cannot finalize") || e.getMessage().contains("cannot sign")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to save document: " + e.getMessage()));
        }
    }

    @PutMapping("/{caseId}/documents/{templateId}/{documentId}")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> updateDocument(
            @PathVariable Long caseId,
            @PathVariable Long templateId,
            @PathVariable Long documentId,
            @Valid @RequestBody CreateCaseDocumentDTO dto,
            HttpServletRequest request) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        if (templateId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Template ID cannot be null"));
        }
        if (documentId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Document ID cannot be null"));
        }
        Long officerId = currentUserService.getCurrentOfficerId(request);
        if (officerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Officer information not found"));
        }
        String roleCode = currentUserService.getCurrentRoleCode(request);
        if (roleCode == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Role information not found"));
        }
        try {
            CaseDocumentDTO saved = documentService.updateDocumentByTemplateId(caseId, templateId, documentId, officerId, dto, roleCode);
            return ResponseEntity.ok(ApiResponse.success("Document updated", saved));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("cannot finalize") || e.getMessage().contains("cannot sign")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update document: " + e.getMessage()));
        }
    }
}

