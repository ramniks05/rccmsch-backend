package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** One entry for allowed document stages in a workflow permission. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllowedDocumentStageEntryDTO {
    private Long documentId;
    private List<String> stages;
}
