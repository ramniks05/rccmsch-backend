package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Officer Entity (Government Employee)
 * Represents a government employee/officer in the RCCMS system
 * This includes both Officers and Dealing Assistants (DAs)
 * This is separate from the Citizen entity which is for Citizens/Operators
 */
@Entity
@Table(name = "officers", uniqueConstraints = {
    @UniqueConstraint(columnNames = "mobile_no", name = "uk_officer_mobile"),
    @UniqueConstraint(columnNames = "email", name = "uk_officer_email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Officer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "mobile_no", nullable = false, length = 10, unique = true)
    private String mobileNo;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash; // BCrypt hash

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    private AuthType authType = AuthType.TEMP_PASSWORD;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_password_reset_required", nullable = false)
    private Boolean isPasswordResetRequired = true; // First login flag

    @Column(name = "is_mobile_verified", nullable = false)
    private Boolean isMobileVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Authentication Type Enum
     */
    public enum AuthType {
        TEMP_PASSWORD,  // Initial temporary password
        OTP,            // OTP-based login (future)
        SSO             // Single Sign-On (future - NIC/e-Pramaan/LDAP)
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (isPasswordResetRequired == null) {
            isPasswordResetRequired = true;
        }
        if (isMobileVerified == null) {
            isMobileVerified = false;
        }
        if (authType == null) {
            authType = AuthType.TEMP_PASSWORD;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

