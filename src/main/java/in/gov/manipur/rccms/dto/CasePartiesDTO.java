package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for case parties (petitioner, respondent) information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasePartiesDTO {
    /**
     * Case ID
     */
    private Long caseId;
    
    /**
     * Case number
     */
    private String caseNumber;
    
    /**
     * List of parties (petitioner, respondent, etc.)
     */
    private List<PartyInfoDTO> parties;

    /**
     * Latest hearing date for this case (derived from case fields or latest HEARING form).
     */
    private LocalDate latestHearingDate;

    /**
     * Latest HEARING module form submission id for linking.
     * Can be null when hearing date comes only from case-level fields.
     */
    private Long latestHearingSubmissionId;
}
