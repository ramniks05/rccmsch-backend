package in.gov.manipur.rccms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class HearingShiftPreviewResponseDTO {
    private LocalDate fromDate;
    private LocalDate toDate;
    private Boolean shiftAll;
    private Integer totalEligibleCases;
    private Integer totalSelectedCases;
    private List<HearingRescheduleCaseDTO> cases;
}
