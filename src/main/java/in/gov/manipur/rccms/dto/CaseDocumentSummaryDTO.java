package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.DocumentStatus;
import in.gov.manipur.rccms.entity.ModuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Summary of a case document for case-detail listing (e.g. NOTICE, ORDERSHEET, JUDGEMENT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseDocumentSummaryDTO {
    private Long documentId;
    private ModuleType moduleType;
    private String moduleTypeLabel;
    private DocumentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime signedAt;
    /** Full document available at GET /api/cases/{caseId}/documents/{templateId} */
    private boolean hasContent;
}
