package in.gov.manipur.rccms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class HearingShiftExecuteResponseDTO {
    private String batchId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer shiftedCount;
    private List<Long> shiftedCaseIds;
}
