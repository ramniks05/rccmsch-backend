package in.gov.manipur.rccms.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Update System Settings DTO
 * Request DTO for updating system settings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSystemSettingsDTO {

    // Header Settings
    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String logoUrl;

    @Size(max = 200, message = "Logo header must not exceed 200 characters")
    private String logoHeader;

    @Size(max = 200, message = "Logo header must not exceed 200 characters")
    private String logoSubheader;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String secondaryLogoUrl;

    @Size(max = 200, message = "Logo header must not exceed 200 characters")
    private String secondaryLogoHeader;

    @Size(max = 500, message = "Logo URL must not exceed 500 characters")
    private String tertiaryLogoUrl;

    @Size(max = 200, message = "Logo header must not exceed 200 characters")
    private String tertiaryLogoHeader;

    @Size(max = 1000, message = "Marquee text must not exceed 1000 characters")
    private String marqueeText;

    @Size(max = 100, message = "State name must not exceed 100 characters")
    private String stateName;

    // Footer Settings
    @Size(max = 500, message = "Footer text must not exceed 500 characters")
    private String footerText;

    @Size(max = 200, message = "Footer copyright must not exceed 200 characters")
    private String footerCopyright;

    @Size(max = 500, message = "Footer address must not exceed 500 characters")
    private String footerAddress;

    @Size(max = 100, message = "Footer email must not exceed 100 characters")
    private String footerEmail;

    @Size(max = 50, message = "Footer phone must not exceed 50 characters")
    private String footerPhone;

    @Size(max = 200, message = "Footer website must not exceed 200 characters")
    private String footerWebsite;

    private List<BannerDTO> banners;
}

