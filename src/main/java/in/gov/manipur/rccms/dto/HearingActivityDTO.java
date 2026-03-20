package in.gov.manipur.rccms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class HearingActivityDTO {
    private Long caseId;
    private Long hearingSubmissionId;
    private LocalDate hearingDate;
    private ModuleFormSubmissionDTO hearingSubmission;
    private List<ModuleFormSubmissionDTO> linkedModuleSubmissions;
}
