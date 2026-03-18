package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request payload for separate attendance submission API.
 * Keeps compatibility with existing form-style payload:
 * {"formData":"{...json...}","remarks":"..."}
 */
@Data
public class SubmitCaseAttendanceDTO {

    @NotBlank
    private String formData;

    /**
     * Link attendance to latest hearing submission id from /api/cases/{caseId}/parties.
     * Required to prevent duplicate attendance for the same hearing.
     */
    private Long latestHearingSubmissionId;

    private String remarks;
}

