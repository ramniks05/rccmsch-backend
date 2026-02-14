package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One case that requires citizen action (for dashboard list).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenActionItemDTO {
    private Long caseId;
    private String caseNumber;
    private String subject;
    /** Action required: code for frontend routing (e.g. ACKNOWLEDGE_NOTICE, RESUBMIT_AFTER_CORRECTION) */
    private String actionCode;
    /** Human-readable label (e.g. "Acknowledge notice", "Resubmit after correction") */
    private String actionLabel;
}
