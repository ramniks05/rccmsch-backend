package in.gov.manipur.rccms.dto;

import in.gov.manipur.rccms.entity.DocumentsAvailable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class DocumentAvailableDTO {

    private Long documentId;
    private String title;
    private LocalDate publishedOn;
    private String filePath;


    public DocumentAvailableDTO(DocumentsAvailable documentsAvailable) {
        this.documentId = documentsAvailable.getDocumentId() != null ? documentsAvailable.getDocumentId() : null;
        this.title = documentsAvailable.getTitle() != null ? documentsAvailable.getTitle() : null;
        this.publishedOn = documentsAvailable.getPublishedOn() != null ? documentsAvailable.getPublishedOn() : null;
        this.filePath = documentsAvailable.getFilePath() != null ? documentsAvailable.getFilePath() : null;

    }

}
