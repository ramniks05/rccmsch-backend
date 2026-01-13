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

import java.time.LocalDate;
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
           @Index(name = "idx_aadhar", columnList = "aadhar_number")
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

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @NotBlank(message = "Address is required")
    @Size(min = 10, max = 500, message = "Address must be between 10 and 500 characters")
    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @NotBlank(message = "District is required")
    @Size(max = 100, message = "District must not exceed 100 characters")
    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^\\d{6}$", message = "Pincode must be exactly 6 digits")
    @Column(name = "pincode", nullable = false, length = 6)
    private String pincode;

    @Column(name = "aadhar_number", nullable = false, unique = true, length = 500) // Length 500 for encrypted value
    private String aadharNumber; // Will be encrypted at rest - validation done at DTO level

    @Column(name = "password", nullable = false, length = 255) // Length 255 for BCrypt hash
    private String password; // Will be hashed with BCrypt

    @Enumerated(EnumType.STRING)
    @Column(name = "citizen_type", nullable = false, length = 20)
    private CitizenType citizenType;

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
     * Gender Enum
     */
    public enum Gender {
        MALE, FEMALE, OTHER
    }

    /**
     * Citizen Type Enum
     */
    public enum CitizenType {
        CITIZEN, OPERATOR
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

