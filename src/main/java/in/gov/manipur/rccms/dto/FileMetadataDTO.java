package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for file metadata in field report submission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataDTO {
    private String fileId; // Temporary ID from frontend
    private String fileName; // Display name or original filename
    private Long fileSize; // File size in bytes
    private String fileType; // MIME type (e.g., "application/pdf")
}
