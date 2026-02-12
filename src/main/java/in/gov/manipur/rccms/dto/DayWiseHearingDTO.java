package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Hearing count and case numbers for a single date (used in next-10-days-by-court response).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayWiseHearingDTO {
    private LocalDate date;
    private int caseCount;
    private List<String> caseNumbers;
}
