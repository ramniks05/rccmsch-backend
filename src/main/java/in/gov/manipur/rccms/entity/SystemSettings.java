package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * System Settings Entity
 * Stores common system settings like logo, header, footer, state name, etc.
 * Singleton pattern - only one settings record exists
 */
@Entity
@Table(name = "system_settings", indexes = {
    @Index(name = "idx_system_settings_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Header Settings
    @Column(name = "logo_url", length = 500)
    private String logoUrl; // URL or path to logo image

    @Column(name = "logo_header", length = 200)
    private String logoHeader; // Header text (e.g., "Revenue & Settlement Department")

    @Column(name = "logo_subheader", length = 200)
    private String logoSubheader; // Subheader text (e.g., "Government of Manipur")

    @Column(name = "state_name", length = 100)
    private String stateName; // State name (e.g., "Manipur")

    // Footer Settings
    @Column(name = "footer_text", length = 500)
    private String footerText; // Footer text content

    @Column(name = "footer_copyright", length = 200)
    private String footerCopyright; // Copyright text

    @Column(name = "footer_address", length = 500)
    private String footerAddress; // Footer address

    @Column(name = "footer_email", length = 100)
    private String footerEmail; // Footer contact email

    @Column(name = "footer_phone", length = 50)
    private String footerPhone; // Footer contact phone

    @Column(name = "footer_website", length = 200)
    private String footerWebsite; // Footer website URL

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

