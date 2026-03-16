package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Transition Checklist
 * Shows which conditions are met and which are blocking a transition.
 * Includes permission-level document/form allowances (allowedDocumentIds, allowDocumentDraft, etc.) so the UI can show what is required.
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

    /** Form IDs (case type / module form) this permission allows. From permission conditions. */
    private List<Long> allowedFormIds;
    /** Document template IDs this permission allows. From permission conditions. */
    private List<Long> allowedDocumentIds;
    /** Whether this permission allows document draft. */
    private Boolean allowDocumentDraft;
    /** Whether this permission allows document save & sign. */
    private Boolean allowDocumentSaveAndSign;
}
