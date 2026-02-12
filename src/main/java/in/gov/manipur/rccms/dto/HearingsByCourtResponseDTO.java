package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Response for hearings by court: next 10 days date-wise hearing count and case numbers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HearingsByCourtResponseDTO {
    private Long courtId;
    private String courtCode;
    private String courtName;
    private LocalDate fromDate;
    private List<DayWiseHearingDTO> days;
}
