package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
