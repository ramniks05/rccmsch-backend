package in.gov.manipur.rccms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * OTP Entity
 * Stores OTP codes for citizen authentication
 */
@Entity
@Table(name = "otps", indexes = {
        @Index(name = "idx_mobile_otp", columnList = "mobile_number,otp_code"),
        @Index(name = "idx_email_otp", columnList = "email_id,otp_code")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mobile_number", nullable = false, length = 10)
    private String mobileNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "citizen_type", nullable = false, length = 20)
    private CitizenType citizenType;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Citizen Type Enum (same as Citizen entity)
     */
    public enum CitizenType {
        CITIZEN, OPERATOR, RESPONDENT, LAWYER
    }

    @PrePersist
    protected void onCreate() {
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(5); // OTP expires in 5 minutes
        }
        if (isUsed == null) {
            isUsed = false;
        }
    }
}

