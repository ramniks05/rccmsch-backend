package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for file information in field report submission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfoDTO {
    private String fieldName; // Form field name (e.g., "supporting_documents")
    private String fileId; // Temporary ID (matches FileMetadataDTO.fileId)
    private String displayName; // User-entered file type/display name
    private String originalFileName; // Original filename from user's computer
}
