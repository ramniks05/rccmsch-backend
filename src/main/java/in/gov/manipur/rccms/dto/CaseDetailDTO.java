package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full case detail on demand: case info, workflow history, and documents summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseDetailDTO {
    private CaseDTO caseInfo;
    private List<WorkflowHistoryDTO> history;
    /** Documents available for this case (notice, ordersheet, judgement, etc.) */
    private List<CaseDocumentSummaryDTO> documents;
}
