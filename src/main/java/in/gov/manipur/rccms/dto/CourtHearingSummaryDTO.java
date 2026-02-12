package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Court-wise hearing summary: court details + list of cases (for a given date).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourtHearingSummaryDTO {
    private Long courtId;
    private String courtCode;
    private String courtName;
    private int caseCount;
    private List<CaseNumberItemDTO> cases;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CaseNumberItemDTO {
        private Long caseId;
        private String caseNumber;
    }
}
