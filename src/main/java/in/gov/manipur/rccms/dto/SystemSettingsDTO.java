package in.gov.manipur.rccms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * System Settings DTO
 * Response DTO for system settings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingsDTO {

    private Long id;

    // Header Settings
    private String logoUrl;
    private String logoHeader;
    private String logoSubheader;
    private String stateName;

    // Footer Settings
    private String footerText;
    private String footerCopyright;
    private String footerAddress;
    private String footerEmail;
    private String footerPhone;
    private String footerWebsite;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

