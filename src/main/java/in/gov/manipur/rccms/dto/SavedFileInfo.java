package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for saved file information after upload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedFileInfo {
    private String fileId; // Generated unique file ID
    private String originalFileId; // Temporary ID from frontend
    private String fieldName; // Form field name
    private String displayName; // Display name
    private String originalFileName; // Original filename
    private String fileUrl; // Saved file URL path
    private Long fileSize; // File size in bytes
    private String fileType; // MIME type
}
