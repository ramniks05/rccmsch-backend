package in.gov.manipur.rccms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class HearingRescheduleCaseDTO {
    private Long caseId;
    private String caseNumber;
    private Long courtId;
    private String courtName;
    private LocalDate currentHearingDate;
    private Integer currentHearingNo;
}
