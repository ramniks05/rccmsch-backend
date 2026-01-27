package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Transition Checklist
 * Shows which conditions are met and which are blocking a transition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionChecklistDTO {
    
    private String transitionCode;
    private String transitionName;
    private Boolean canExecute;
    private List<ConditionStatusDTO> conditions;
    private List<String> blockingReasons;
}
