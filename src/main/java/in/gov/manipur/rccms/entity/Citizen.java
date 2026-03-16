package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Citizen Entity (Citizen/Operator)
 * Represents a citizen or operator in the RCCMS system
 */
@Entity
@Table(name = "citizens", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = "email"),
           @UniqueConstraint(columnNames = "mobile_number"),
           @UniqueConstraint(columnNames = "aadhar_number")
       },
       indexes = {
           @Index(name = "idx_email", columnList = "email"),
           @Index(name = "idx_mobile", columnList = "mobile_number"),
           @Index(name = "idx_aadhar", columnList = "aadhar_number"),
           @Index(name = "idx_citizen_role", columnList = "role_id")
       })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Citizen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "First name must contain only letters")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Last name must contain only letters")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email format")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be 10 digits starting with 6-9")
    @Column(name = "mobile_number", nullable = false, unique = true, length = 10)
    private String mobileNumber;

    @Column(name = "password", nullable = false, length = 255) // Length 255 for BCrypt hash
    private String password; // Will be hashed with BCrypt

    @Column(name = "registration_data", columnDefinition = "TEXT")
    private String registrationData; // JSON string for dynamic registration fields

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_citizen_role"))
    private RoleMaster role; // CITIZEN, RESPONDENT, OPERATOR from role_master

    @Column(name = "role_id", insertable = false, updatable = false)
    private Long roleId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false; // Set to true after mobile verification

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;

    @Column(name = "is_mobile_verified", nullable = false)
    private Boolean isMobileVerified = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Citizen Type Enum (matches role_master.role_code for citizen-facing types).
     * Used in API/OTP; source of truth is role (RoleMaster).
     */
    public enum CitizenType {
        CITIZEN, OPERATOR, RESPONDENT, LAWYER
    }

    /**
     * Derive citizen type from role_master (no stored citizen_type column).
     */
    public CitizenType getCitizenType() {
        if (role == null) return CitizenType.CITIZEN;
        String code = role.getRoleCode();
        if (code == null) return CitizenType.CITIZEN;
        try {
            return CitizenType.valueOf(code);
        } catch (IllegalArgumentException e) {
            return CitizenType.CITIZEN;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (isActive == null) {
            isActive = false; // Will be set to true after mobile verification
        }
        if (isEmailVerified == null) {
            isEmailVerified = false;
        }
        if (isMobileVerified == null) {
            isMobileVerified = false;
        }
    }
}

