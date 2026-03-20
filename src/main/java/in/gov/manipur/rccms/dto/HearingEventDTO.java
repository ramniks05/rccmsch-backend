package in.gov.manipur.rccms.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class HearingEventDTO {
    private Long id;
    private Long caseId;
    private Long hearingSubmissionId;
    private Integer hearingNo;
    private LocalDate hearingDate;
    private String status;
    private Boolean isCurrent;
    private Long previousHearingEventId;
    private String source;
    private String batchId;
    private String reason;
    private String remarks;
    private Long actionByOfficerId;
    private LocalDateTime actionAt;
}
