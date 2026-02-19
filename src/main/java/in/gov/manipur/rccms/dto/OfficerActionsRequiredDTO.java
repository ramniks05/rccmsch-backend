package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Officer dashboard "Actions required" response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfficerActionsRequiredDTO {
    /** Total number of assigned cases that have at least one available transition */
    private long totalCount;
    /** Optional short list; empty if not requested */
    private List<OfficerActionItemDTO> items;
}
