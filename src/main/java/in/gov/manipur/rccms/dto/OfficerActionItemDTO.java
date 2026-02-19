package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One case that requires officer action (for dashboard list).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficerActionItemDTO {
    private Long caseId;
    private String caseNumber;
    private String subject;
    private String currentStateCode;
    private String currentStateName;
    /** Available transitions (code and label) for this case */
    private List<TransitionSummaryDTO> availableTransitions;
}
