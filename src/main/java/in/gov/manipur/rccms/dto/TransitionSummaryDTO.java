package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transition code and label for dropdowns and lists.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionSummaryDTO {
    private String code;
    private String label;
}
