package in.gov.manipur.rccms.controller;

import in.gov.manipur.rccms.dto.ApiResponse;
import in.gov.manipur.rccms.dto.CaseDocumentDTO;
import in.gov.manipur.rccms.dto.CreateCaseDocumentDTO;
import in.gov.manipur.rccms.dto.DocumentTemplateDTO;
import in.gov.manipur.rccms.entity.Case;
import in.gov.manipur.rccms.entity.ModuleType;
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

    @GetMapping("/{caseId}/documents/{moduleType}/template")
    public ResponseEntity<ApiResponse<DocumentTemplateDTO>> getActiveTemplate(
            @PathVariable Long caseId,
            @PathVariable ModuleType moduleType) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found: " + caseId));
        DocumentTemplateDTO template = templateService.getLatestActiveTemplate(
                caseEntity.getCaseNatureId(), caseEntity.getCaseTypeId(), moduleType);
        return ResponseEntity.ok(ApiResponse.success("Template retrieved", template));
    }

    @GetMapping("/{caseId}/documents/{moduleType}")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> getDocument(
            @PathVariable Long caseId,
            @PathVariable ModuleType moduleType) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        CaseDocumentDTO doc = documentService.getLatestDocument(caseId, moduleType);
        return ResponseEntity.ok(ApiResponse.success("Document retrieved", doc));
    }

    @GetMapping("/{caseId}/documents/{moduleType}/latest")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> getLatestDocument(
            @PathVariable Long caseId,
            @PathVariable ModuleType moduleType) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
        }
        CaseDocumentDTO doc = documentService.getLatestDocument(caseId, moduleType);
        return ResponseEntity.ok(ApiResponse.success("Document retrieved", doc));
    }

    @PostMapping("/{caseId}/documents/{moduleType}")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> createOrUpdateDocument(
            @PathVariable Long caseId,
            @PathVariable ModuleType moduleType,
            @Valid @RequestBody CreateCaseDocumentDTO dto,
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
        CaseDocumentDTO saved = documentService.createOrUpdateDocument(caseId, moduleType, officerId, dto);
        return ResponseEntity.ok(ApiResponse.success("Document saved", saved));
    }

    @PutMapping("/{caseId}/documents/{moduleType}/{documentId}")
    public ResponseEntity<ApiResponse<CaseDocumentDTO>> updateDocument(
            @PathVariable Long caseId,
            @PathVariable ModuleType moduleType,
            @PathVariable Long documentId,
            @Valid @RequestBody CreateCaseDocumentDTO dto,
            HttpServletRequest request) {
        if (caseId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Case ID cannot be null"));
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
        CaseDocumentDTO saved = documentService.updateDocument(caseId, moduleType, documentId, officerId, dto);
        return ResponseEntity.ok(ApiResponse.success("Document updated", saved));
    }
}

