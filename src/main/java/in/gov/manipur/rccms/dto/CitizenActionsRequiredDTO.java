package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Citizen dashboard "Actions required" response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenActionsRequiredDTO {
    /** Total number of cases needing citizen action */
    private long totalCount;
    /** Optional short list (e.g. first 10); empty if not requested */
    private List<CitizenActionItemDTO> items;
}
