package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Response for hearings by date: all courts with case numbers for the selected date.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HearingsByDateResponseDTO {
    private LocalDate date;
    private List<CourtHearingSummaryDTO> courts;
}
