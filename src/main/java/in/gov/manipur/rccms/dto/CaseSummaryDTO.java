package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dashboard case summary counts (total, pending, disposed, hearing scheduled).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseSummaryDTO {
    private long totalCases;
    private long pendingCases;
    private long disposedCases;
    private long hearingScheduledCount;
    private long totalCourts;
}
