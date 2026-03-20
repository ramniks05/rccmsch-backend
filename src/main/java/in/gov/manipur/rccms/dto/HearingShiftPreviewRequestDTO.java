package in.gov.manipur.rccms.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class HearingShiftPreviewRequestDTO {
    private LocalDate fromDate;
    private LocalDate toDate;
    private Long courtId;
    private Boolean shiftAll = false;
    private List<Long> caseIds;
}
